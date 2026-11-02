package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.response.CentralPortabilityStatusResponse;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.CentralIntegrationException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortabilityStatusServiceTest {

    @Mock
    private PortabilityRepository repository;

    @Mock
    private CentralClient centralClient;

    @Mock
    private PortabilityOperationPersistenceService persistenceService;

    @InjectMocks
    private PortabilityStatusService service;

    private UUID operationId;
    private Portability pending;

    @BeforeEach
    void setUp() {
        operationId = UUID.randomUUID();
        pending = operation(PortabilityStatus.PENDENTE, null);
    }

    @Test
    public void shouldReturnPendingWhileCentralIsProcessing(){
        stubPendingOperation();

        when(centralClient.getPortabilityStatus("PROTOCOL001"))
                .thenReturn(new CentralPortabilityStatusResponse(
                        PortabilityStatus.PENDENTE,
                        null
                ));

        when(persistenceService.applyCentralStatus(
                operationId,
                PortabilityStatus.PENDENTE,
                null
        )).thenReturn(pending);

        var result = service.getStatus(operationId);

        assertSame(pending, result);
        assertEquals(PortabilityStatus.PENDENTE, result.getStatus());
        assertNull(result.getRejectionReason());
    }

    @Test
    public void shouldApplyRejectedStatusAndRejectionReason() {
        stubPendingOperation();

        String reason = "Beneficiário atingiu o limite de margem consignável";
        var rejected = operation(PortabilityStatus.REJEITADA, reason);

        when(centralClient.getPortabilityStatus("PROTOCOL001"))
                .thenReturn(new CentralPortabilityStatusResponse(
                        PortabilityStatus.REJEITADA,
                        reason
                ));

        when(persistenceService.applyCentralStatus(
                operationId,
                PortabilityStatus.REJEITADA,
                reason
        )).thenReturn(rejected);

        var result = service.getStatus(operationId);

        assertSame(rejected, result);
        assertEquals(PortabilityStatus.REJEITADA, result.getStatus());
        assertEquals(reason, result.getRejectionReason());

        verify(persistenceService).applyCentralStatus(
                operationId,
                PortabilityStatus.REJEITADA,
                reason
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = PortabilityStatus.class,
            names = {"CONFIRMADA", "REJEITADA"})
    public void shouldNotConsultCentralWhenOperationIsAlreadyFinished(PortabilityStatus status) {
        var finished = operation(status, null);
        when(repository.findByIdAndRequestHashIsNotNull(operationId))
                .thenReturn(Optional.of(finished));

        var result = service.getStatus(operationId);

        assertSame(finished, result);
        verifyNoInteractions(centralClient, persistenceService);
    }

    @Test
    void shouldNotUpdateOperationWhenCentralCommunicationFails() {
        stubPendingOperation();
        var failure = new CentralIntegrationException(
                "Não foi possível consultar o status na Central.",
                new ResourceAccessException("Read timed out")
        );
        when(centralClient.getPortabilityStatus("PROTOCOL001"))
                .thenThrow(failure);

        var exception = assertThrows(
                CentralIntegrationException.class,
                () -> service.getStatus(operationId)
        );
        assertSame(failure, exception);
        assertEquals(PortabilityStatus.PENDENTE, pending.getStatus());

        verifyNoInteractions(persistenceService);
    }

    @Test
    void shouldThrowResourceNotFoundWhenOperationDoesNotExist() {
        when(repository.findByIdAndRequestHashIsNotNull(operationId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFound.class,
                () -> service.getStatus(operationId)
        );
        verifyNoInteractions(centralClient, persistenceService);
    }

    private void stubPendingOperation() {
        when(repository.findByIdAndRequestHashIsNotNull(operationId))
                .thenReturn(Optional.of(pending));
    }

    private Portability operation(
            PortabilityStatus status,
            String rejectionReason
    ) {
        return Portability.builder()
                .id(operationId)
                .beneficiaryId("BEN001")
                .protocolCentral("PROTOCOL001")
                .status(status)
                .rejectionReason(rejectionReason)
                .build();
    }


}
