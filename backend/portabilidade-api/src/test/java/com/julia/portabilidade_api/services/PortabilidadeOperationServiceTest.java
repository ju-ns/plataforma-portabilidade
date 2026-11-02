package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.response.CentralPortabilityResponse;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortabilidadeOperationServiceTest {

    @Mock
    private PortabilityRepository repository;

    @Mock
    private CentralClient centralClient;

    @Mock
    private PortabilityOperationPersistenceService persistenceService;

    @InjectMocks
    private PortabilityOperationService service;

    private CreatePortabilityRequest request;
    private Portability reserved;
    private UUID operationId;

    @BeforeEach
    void setUp(){
        operationId = UUID.randomUUID();

        request = new CreatePortabilityRequest(
                "BEN001",
                List.of("P001", "P002"),
                "001",
                "1234",
                "123456-7",
                "port-ben001-001"
        );

        reserved = operation(PortabilityStatus.CRIADA, null);
    }


    @Test
    public void shouldReserveOperationOperationBeforeSubmittingToCentral(){
        stubSuccessfulCreation();
        var result = service.create(request);

        assertEquals(operationId, result.getId());
        assertEquals(PortabilityStatus.PENDENTE, result.getStatus());
        assertEquals("PROTOCOL001", result.getProtocolCentral());

        var order = inOrder(persistenceService, centralClient);
        order.verify(persistenceService).reserve(
                eq(request),
                anyString(),
                eq("AUTH001")
        );
        order.verify(centralClient).createPortability(
                request,
                "AUTH001"
        );
        order.verify(persistenceService).markPending(
                operationId,
                "PROTOCOL001"
        );

        verify(persistenceService, never()).markUncertain(any());
        verify(centralClient, never()).getSchedule(anyString());
    }

    @Test
    public void shouldNotCreateOperationWithoutAuthorization(){
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.empty());
        when(repository.findFirstByBeneficiaryIdAndAuthorizedAtIsNotNullOrderByAuthorizedAtDesc(request.beneficiaryId()))
                .thenReturn(Optional.empty());
        assertThrows(
                BeneficiaryNotAuthorizedException.class,
                () -> service.create(request)
        );
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldReuseOperationWithoutSubmittingAgain(){
        var existing = prepareExistingOperation();
        var result = service.create(request);
        assertSame(existing, result);
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldReuseOperationWhenInstallmentOrderChanges(){
        var existing = prepareExistingOperation();
        var reorderedRequest = new CreatePortabilityRequest(
                request.beneficiaryId(),
                List.of("P002", "P001"),
                request.destinationBank(),
                request.destinationBranch(),
                request.destinationAccount(),
                request.idempotencyKey()
        );
        var result = service.create(reorderedRequest);
        assertSame(existing, result);
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldRejectSameKeyWithAnotherDestinationAccount(){
        prepareExistingOperation();

        var changedRequest = new CreatePortabilityRequest(
                request.beneficiaryId(),
                request.installmentIds(),
                request.destinationBank(),
                request.destinationBranch(),
                "999999-9",
                request.idempotencyKey()
        );
        assertThrows(
                DuplicateException.class,
                () -> service.create(changedRequest)
        );
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldRejectSameKeyWithAnotherInstallmentSection(){
        prepareExistingOperation();

        var changedRequest = new CreatePortabilityRequest(
                request.beneficiaryId(),
                List.of("P001"),
                request.destinationBank(),
                request.destinationBranch(),
                request.destinationAccount(),
                request.idempotencyKey()
        );

        assertThrows(
                DuplicateException.class,
                () -> service.create(changedRequest)
        );
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldNotResubmitOperationWithUncertainResult() {
        var existing = prepareExistingOperation();
        when(repository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.of(
                        operation(
                                PortabilityStatus.PORTABILIDADE_INCERTA,
                                existing.getRequestHash()
                        )
                ));
        assertThrows(PortabilityUncertainException.class,
                () -> service.create(request));
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    public void shouldTranslateCentral503IntoCentralUnavailableException(){
        stubReservation();

        var failure = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        when(centralClient.createPortability(request, "AUTH001"))
                .thenThrow(failure);
        var exception = assertThrows(
                CentralUnavailableException.class,
                () -> service.create(request)
        );
        assertInstanceOf(PortabilityUncertainException.class, exception.getCause());
        assertSame(failure, exception.getCause().getCause());
        verify(persistenceService).markUncertain(operationId);
        verify(persistenceService, never()).markPending(any(), any());
    }

    @Test
    public void shouldMarkOperationUncertainWhenCentralCommunicationFails() {
        stubReservation();

        var failure = new ResourceAccessException("Read timed out");

        when(centralClient.createPortability(request, "AUTH001"))
                .thenThrow(failure);
        var exception = assertThrows(
                PortabilityUncertainException.class,
                () -> service.create(request)
        );
        assertSame(failure, exception.getCause());

        verify(persistenceService).markUncertain(operationId);
        verify(persistenceService, never()).markPending(any(), any());
    }

    @Test
    public void shouldMarkOperationUncertainWhenCentralResponseIsInvalid() {
        stubReservation();

        var failure = new InvalidCentralResponseException(
                "A Central retornou um protocolo inválido."
        );
        when(centralClient.createPortability(request, "AUTH001"))
                .thenThrow(failure);
        var exception = assertThrows(
                PortabilityUncertainException.class,
                () -> service.create(request)
        );
        assertSame(failure, exception.getCause());

        verify(persistenceService).markUncertain(operationId);
        verify(persistenceService, never()).markPending(any(), any());
    }

    @Test
    public void shouldReuseOperationWhenAnotherRequestReservesKeyFirst(){
        var existing = prepareExistingOperation();
        when(repository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty(), Optional.of(existing));

        when(persistenceService.reserve(
                eq(request),
                anyString(),
                eq("AUTH001")
        )).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        var result = service.create(request);

        assertSame(existing, result);

        verifyNoInteractions(centralClient);
        verify(persistenceService, never()).markPending(any(), any());
    }

    private void stubReservation(){
        when(repository.findByIdempotencyKey(request.idempotencyKey())).thenReturn(Optional.empty());
        var authorization = Portability.builder()
                .beneficiaryId(request.beneficiaryId())
                .authorizationId("AUTH001")
                .status(PortabilityStatus.AUTORIZADA)
                .build();
        when(repository.findFirstByBeneficiaryIdAndAuthorizedAtIsNotNullOrderByAuthorizedAtDesc(request.beneficiaryId()))
                .thenReturn(Optional.of(authorization));
        when(persistenceService.reserve(eq(request),
                anyString(),
                eq("AUTH001")
        )).thenReturn(reserved);
    }

    private void stubSuccessfulCreation(){
        stubReservation();

        when(centralClient.createPortability(request, "AUTH001"))
                .thenReturn(new CentralPortabilityResponse("PROTOCOL001"));
        var pending = operation(PortabilityStatus.PENDENTE, null);
        pending.markPending("PROTOCOL001");
        when(persistenceService.markPending(operationId, "PROTOCOL001"))
                .thenReturn(pending);
    }

    private Portability prepareExistingOperation(){
        stubSuccessfulCreation();
        service.create(request);

        var hashCaptor = ArgumentCaptor.forClass(String.class);

        verify(persistenceService).reserve(
                eq(request),
                hashCaptor.capture(),
                eq("AUTH001")
        );

        var existing = operation(
                PortabilityStatus.PENDENTE,
                hashCaptor.getValue()
        );

        existing.markPending("PROTOCOL001");
        when(repository.findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.of(existing));
        clearInvocations(repository, centralClient, persistenceService);
        return existing;
    }

    private Portability operation(
            PortabilityStatus status,
            String requestHash
    ) {
        return Portability.builder()
                .id(operationId)
                .beneficiaryId(request.beneficiaryId())
                .authorizationId("AUTH001")
                .idempotencyKey(request.idempotencyKey())
                .requestHash(requestHash)
                .status(status)
                .build();
    }
}
