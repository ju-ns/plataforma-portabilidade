package com.julia.portabilidade_api.controllers;

import com.julia.portabilidade_api.controllers.handlers.GlobalExceptionHandler;
import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.services.PortabilityOperationService;
import com.julia.portabilidade_api.services.PortabilityStatusService;
import com.julia.portabilidade_api.services.exceptions.CentralUnavailableException;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PortabilityOperationControllerTest {

    private static final UUID OPERATION_ID = UUID.fromString(
            "9be367f2-883c-4be1-b0cc-5031d537083b"
    );

    private static final String REQUEST_JSON = """
            {
              "beneficiaryId": "BEN001",
              "installmentIds": ["P001"],
              "destinationBank": "001",
              "destinationBranch": "1234",
              "destinationAccount": "123456-7",
              "idempotencyKey": "port-001"
            }
            """;
    @Mock
    private PortabilityOperationService service;

    @Mock
    private PortabilityStatusService statusService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PortabilityOperationController(service, statusService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldReturn202WhenOperationIsAccepted() throws Exception {
        when(service.create(any(CreatePortabilityRequest.class)))
                .thenReturn(operation(PortabilityStatus.PENDENTE, null));

        mockMvc.perform(post("/portabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(OPERATION_ID.toString()))
                .andExpect(jsonPath("$.protocol").value("PROTOCOL001"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void shouldReturn422WhenInstallmentSelectionIsEmpty() throws Exception {
        String invalidRequest = REQUEST_JSON.replace(
                "[\"P001\"]",
                "[]"
        );

        mockMvc.perform(post("/portabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturn409ForIdempotencyConflict() throws Exception {
        String message =
                "Esta chave já foi utilizada com outra solicitação.";

        when(service.create(any(CreatePortabilityRequest.class)))
                .thenThrow(new DuplicateException(message));

        mockMvc.perform(post("/portabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(message));
    }

    @Test
    void shouldReturn503WhenCentralIsUnavailable() throws Exception {
        String message = "A Central está indisponível no momento.";

        when(service.create(any(CreatePortabilityRequest.class)))
                .thenThrow(new CentralUnavailableException(
                        message,
                        new RuntimeException("Central returned HTTP 503")
                ));

        mockMvc.perform(post("/portabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON.replace("BEN001", "BEN_ERRO")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value(message))
                .andExpect(jsonPath("$.path").value("/portabilities"));
    }

    @Test
    void shouldReturnPendingStatusWithRetryAfter() throws Exception {
        when(statusService.getStatus(OPERATION_ID))
                .thenReturn(operation(PortabilityStatus.PENDENTE, null));

        mockMvc.perform(get("/portabilities/{id}", OPERATION_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @ParameterizedTest
    @EnumSource(
            value = PortabilityStatus.class,
            names = {"CONFIRMADA", "REJEITADA"}
    )
    void shouldReturnFinalStatus(
            PortabilityStatus operationStatus
    ) throws Exception {
        String reason = operationStatus == PortabilityStatus.REJEITADA
                ? "Beneficiário atingiu o limite de margem consignável"
                : null;

        when(statusService.getStatus(OPERATION_ID))
                .thenReturn(operation(operationStatus, reason));

        var result = mockMvc.perform(
                        get("/portabilities/{id}", OPERATION_ID)
                )
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.status")
                        .value(operationStatus.name()));

        if (reason != null) {
            result.andExpect(jsonPath("$.rejectionReason").value(reason));
        }
    }

    private Portability operation(
            PortabilityStatus status,
            String rejectionReason
    ) {
        return Portability.builder()
                .id(OPERATION_ID)
                .beneficiaryId("BEN001")
                .protocolCentral("PROTOCOL001")
                .status(status)
                .rejectionReason(rejectionReason)
                .build();
    }

}
