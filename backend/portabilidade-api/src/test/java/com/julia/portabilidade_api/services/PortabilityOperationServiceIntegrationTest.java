package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.response.CentralPortabilityResponse;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.InstallmentRepository;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:portability-idempotency-tests;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.jpa.show-sql=false"
        }
)
@ActiveProfiles("test")
public class PortabilityOperationServiceIntegrationTest {
        @Autowired
        private PortabilityOperationService service;

        @Autowired
        private PortabilityOperationPersistenceService persistenceService;

        @Autowired
        private PortabilityRepository repository;

        @Autowired
        private InstallmentRepository installmentRepository;

        @MockitoBean
        private CentralClient centralClient;

        private CreatePortabilityRequest request;

        @BeforeEach
        void setUp() {
                installmentRepository.deleteAllInBatch();
                repository.deleteAllInBatch();

                var authorization = Portability.builder()
                        .beneficiaryId("BEN001")
                        .authorizationId("AUTH001")
                        .idempotencyKey("auth-ben001-001")
                        .status(PortabilityStatus.AUTORIZADA)
                        .createdAt(LocalDateTime.now())
                        .authorizedAt(LocalDateTime.now())
                        .build();

                repository.saveAndFlush(authorization);

                request = new CreatePortabilityRequest(
                        "BEN001",
                        List.of("P001", "P002"),
                        "001",
                        "1234",
                        "123456-7",
                        "port-ben001-001"
                );
        }

        @Test
        void shouldPersistOnlyOneOperationForRepeatedRequests() {
                stubCentralSuccess();

                var first = service.create(request);
                var repeated = service.create(request);

                var reorderedRequest = new CreatePortabilityRequest(
                        request.beneficiaryId(),
                        List.of("P002", "P001"),
                        request.destinationBank(),
                        request.destinationBranch(),
                        request.destinationAccount(),
                        request.idempotencyKey()
                );

                var reordered = service.create(reorderedRequest);

                assertEquals(first.getId(), repeated.getId());
                assertEquals(first.getId(), reordered.getId());
                assertEquals("PROTOCOL001", repeated.getProtocolCentral());
                assertEquals(PortabilityStatus.PENDENTE, repeated.getStatus());

                assertEquals(2L, repository.count());
                assertEquals(2L, installmentRepository.count());
                verify(centralClient, times(1)).createPortability(
                        request,
                        "AUTH001"
                );
        }

        @Test
        void shouldRejectSameKeyWithDifferentAccountWithoutCreatingAnotherOperation() {
                stubCentralSuccess();
                service.create(request);

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

                assertEquals(2L, repository.count());
                assertEquals(2L, installmentRepository.count());

                verify(centralClient, times(1)).createPortability(
                        request,
                        "AUTH001"
                );
        }


        @Test
        void shouldKeepOriginalOperationAfterDuplicateReservationRollsBack() {
                stubCentralSuccess();

                var original = service.create(request);
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> persistenceService.reserve(
                                request,
                                original.getRequestHash(),
                                "AUTH001"
                        )
                );
                var recovered = assertDoesNotThrow(
                        () -> service.create(request)
                );

                assertEquals(original.getId(), recovered.getId());
                assertEquals("PROTOCOL001", recovered.getProtocolCentral());
                assertEquals(PortabilityStatus.PENDENTE, recovered.getStatus());

                assertEquals(2L, repository.count());
                assertEquals(2L, installmentRepository.count());

                verify(centralClient, times(1)).createPortability(
                        request,
                        "AUTH001"
                );
        }

        private void stubCentralSuccess() {
                when(centralClient.createPortability(request, "AUTH001"))
                        .thenReturn(new CentralPortabilityResponse("PROTOCOL001"));
        }

}
