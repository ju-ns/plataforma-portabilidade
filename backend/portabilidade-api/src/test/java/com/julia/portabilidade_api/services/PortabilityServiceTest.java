package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.RegisterAuthorizationResponse;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.AuthorizationUncertainException;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import com.julia.portabilidade_api.services.exceptions.InvalidCentralResponseException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortabilityServiceTest {

    @Mock
    private PortabilityRepository repository;

    @Mock
    private CentralClient centralClient;

    @Mock
    private AuthorizationPersistenceService authorizationPersistenceService;

    @InjectMocks
    private PortabilityService service;

    private RegisterAuthorizationRequest request;
    private Portability reserved;
    private String authorizationId;

    @BeforeEach
    void setUp()  {
        authorizationId = "AUTH001";
        request = new RegisterAuthorizationRequest(
                "BEN001",
                "auth-ben001-001"
        );
        reserved = Portability.builder()
                .id(UUID.randomUUID())
                .beneficiaryId(request.beneficiaryId())
                .idempotencyKey(request.idempotencyKey())
                .status(PortabilityStatus.CRIADA)
                .build();
    }

    private void stubReservation(){
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.empty());
        when(authorizationPersistenceService.reserve(request.beneficiaryId(), request.idempotencyKey())).thenReturn(reserved);
    }

    @Test
    public void shouldRegisterAuthorizationAfterReservingKey(){
        stubReservation();
        when(centralClient.registerAuthorization(request)).thenReturn(new RegisterAuthorizationResponse(authorizationId));

        var authorized = Portability.builder()
                .id(reserved.getId())
                .beneficiaryId(request.beneficiaryId())
                .authorizationId(authorizationId)
                .status(PortabilityStatus.AUTORIZADA)
                .build();
        when(authorizationPersistenceService.authorize(reserved.getId(), authorizationId)).thenReturn(authorized);
        var result = service.registerAuthorization(request);

        assertTrue(result.created());
        assertSame(authorized, result.portability());

        var order = inOrder(authorizationPersistenceService, centralClient);

        order.verify(authorizationPersistenceService)
                .reserve(request.beneficiaryId(), request.idempotencyKey());
        order.verify(centralClient).registerAuthorization(request);
        order.verify(authorizationPersistenceService).authorize(
                reserved.getId(),
                authorizationId
        );

        verify(authorizationPersistenceService, never()).markUncertain(any());
    }

    @Test
    public void shouldReuseExistingAuthorizationWithoutCallingCentral(){
        reserved.authorize(authorizationId);
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.of(reserved));
        var result = service.registerAuthorization(request);

        assertFalse(result.created());
        assertSame(reserved, result.portability());
        assertEquals(authorizationId, result.portability().getAuthorizationId());
        verifyNoInteractions(centralClient, authorizationPersistenceService);
    }

    @Test
    public void shouldReuseAuthorizationStillBeingProcessed(){
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.of(reserved));

        var result = service.registerAuthorization(request);
        assertFalse(result.created());
        assertEquals(PortabilityStatus.CRIADA, result.portability().getStatus());

        verifyNoInteractions(centralClient, authorizationPersistenceService);
    }

    @Test
    public void shouldRejectKeyUsedForAnotherBeneficiary(){
        var existing = Portability.builder()
                .beneficiaryId("BEN002")
                .idempotencyKey(request.idempotencyKey())
                .status(PortabilityStatus.AUTORIZADA)
                .build();
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.of(existing));
        assertThrows(DuplicateException.class,
                () -> service.registerAuthorization(request));
        verifyNoInteractions(centralClient, authorizationPersistenceService);
    }

    @Test
    public void shouldNotResubmitAuthorizationWithUncertainResult(){
        var existing = Portability.builder()
                .beneficiaryId(request.beneficiaryId())
                .idempotencyKey(request.idempotencyKey())
                .status(PortabilityStatus.AUTORIZACAO_INCERTA)
                .build();
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.of(existing));
        assertThrows(AuthorizationUncertainException.class,
                () -> service.registerAuthorization(request));
        verifyNoInteractions(centralClient, authorizationPersistenceService);
    }

    @Test
    public void shouldReuseRecordWhenAnotherRequestReservedKeyFirst(){
        reserved.authorize(authorizationId);

        when(repository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty(),Optional.of(reserved));

        when(authorizationPersistenceService.reserve(request.beneficiaryId(), request.idempotencyKey()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        var result = service.registerAuthorization(request);

        assertFalse(result.created());
        assertSame(reserved, result.portability());

        verifyNoInteractions(centralClient);
        verify(authorizationPersistenceService, never()).authorize(any(), any());
    }

    @Test
    public void shouldPropagateIntegrityFailureWhenKeyWasNotDuplicated(){
        var failure = new DataIntegrityViolationException("Another integrity constraint failed");
        when(repository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(authorizationPersistenceService.reserve(request.beneficiaryId(), request.idempotencyKey()))
                .thenThrow(failure);

        var exception = assertThrows(DataIntegrityViolationException.class,
                () -> service.registerAuthorization(request));

        assertSame(failure, exception);
        verifyNoInteractions(centralClient);
    }

    @Test
    public void shouldMarkAuthorizationUncertainWhenCentralCommunicationFalls(){
        stubReservation();
        var failure = new ResourceAccessException("Read time out");
        when(centralClient.registerAuthorization(request)).thenThrow(failure);

        var exception = assertThrows(AuthorizationUncertainException.class,
                () -> service.registerAuthorization(request));

        assertSame(failure, exception.getCause());

        verify(authorizationPersistenceService).markUncertain(reserved.getId());
        verify(authorizationPersistenceService, never()).authorize(any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    public void shouldMarkAuthorizationUncertainWhenAuthorizationIdIsInvalid(String authorizationId){
        stubReservation();
        when(centralClient.registerAuthorization(request)).thenReturn(new RegisterAuthorizationResponse(authorizationId));

        var exception = assertThrows(AuthorizationUncertainException.class,
                ()-> service.registerAuthorization(request));

        assertInstanceOf(InvalidCentralResponseException.class, exception.getCause());
        verify(authorizationPersistenceService).markUncertain(reserved.getId());
        verify(authorizationPersistenceService, never()).authorize(any(), any());

    }

    @Test
    public void shouldMarkAuthorizationUncertainWhenCentralReturnsNoBody(){
        stubReservation();

        when(centralClient.registerAuthorization(request)).thenReturn(null);
        var exception = assertThrows(AuthorizationUncertainException.class,
                () -> service.registerAuthorization(request));

        assertInstanceOf(InvalidCentralResponseException.class, exception.getCause());
        verify(authorizationPersistenceService).markUncertain(reserved.getId());
        verify(authorizationPersistenceService, never()).authorize(any(), any());
    }

    @Test
    public void shouldMarkAuthorizationUncertainWhenSavingCentralResultFails(){
        stubReservation();

        when(centralClient.registerAuthorization(request)).thenReturn(new RegisterAuthorizationResponse(authorizationId));
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(authorizationPersistenceService.authorize(reserved.getId(), authorizationId)).thenThrow(failure);

        var exception = assertThrows(AuthorizationUncertainException.class,
                () -> service.registerAuthorization(request));
        assertSame(failure, exception.getCause());
        verify(authorizationPersistenceService).markUncertain(reserved.getId());

    }

}
