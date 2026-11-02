package com.julia.portabilidade_api.controllers;

import com.julia.portabilidade_api.controllers.handlers.GlobalExceptionHandler;
import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.AuthorizationResult;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.services.PortabilityService;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PortabilityControllerTest {

    private static final String REQUEST_JSON = """
            {
              "beneficiaryId": "BEN001",
              "idempotencyKey": "auth-001"
            }
            """;

    @Mock
    private PortabilityService service;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PortabilityController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturn201ForNewAuthorizationAnd200ForExisting(
            boolean created
    ) throws Exception {
        UUID id = UUID.randomUUID();

        var authorized = Portability.builder()
                .id(id)
                .beneficiaryId("BEN001")
                .authorizationId("AUTH001")
                .status(PortabilityStatus.AUTORIZADA)
                .build();

        when(service.registerAuthorization(
                any(RegisterAuthorizationRequest.class)
        )).thenReturn(new AuthorizationResult(authorized, created));

        mockMvc.perform(post("/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().is(created ? 201 : 200))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.beneficiaryId").value("BEN001"))
                .andExpect(jsonPath("$.authorizationId").value("AUTH001"))
                .andExpect(jsonPath("$.status").value("AUTORIZADA"));
    }

    @Test
    void shouldReturn422ForInvalidFields() throws Exception {
        mockMvc.perform(post("/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "beneficiaryId": "",
                                  "idempotencyKey": ""
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturn409ForIdempotencyConflict() throws Exception {
        String message =
                "Esta chave de idempotência já foi usada para outro beneficiário.";

        when(service.registerAuthorization(
                any(RegisterAuthorizationRequest.class)
        )).thenThrow(new DuplicateException(message));

        mockMvc.perform(post("/authorizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(message));
    }

}
