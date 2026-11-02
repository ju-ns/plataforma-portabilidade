package com.julia.portabilidade_api.controllers;


import com.julia.portabilidade_api.controllers.handlers.GlobalExceptionHandler;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.services.ScheduleService;
import com.julia.portabilidade_api.services.exceptions.BeneficiaryNotAuthorizedException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class BeneficiaryControllerTest {

    @Mock
    private ScheduleService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BeneficiaryController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnScheduleWithInstallmentDetails() throws Exception {
        var installment = new InstallmentResponse(
                "P001",
                "CONV01",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 1, 10),
                "ABERTA"
        );

        when(service.getSchedule("BEN001"))
                .thenReturn(List.of(installment));

        mockMvc.perform(get("/beneficiaries/BEN001/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].installmentId").value("P001"))
                .andExpect(jsonPath("$[0].amount").value(150.0))
                .andExpect(jsonPath("$[0].deductionDate").value("2026-01-10"))
                .andExpect(jsonPath("$[0].status").value("ABERTA"));
    }

    @Test
    void shouldReturn404WhenScheduleDoesNotExist() throws Exception {
        String message =
                "Nenhum agendamento de desconto encontrado para o beneficiário BEN_ERRO.";

        when(service.getSchedule("BEN_ERRO"))
                .thenThrow(new ResourceNotFound(message));

        mockMvc.perform(get("/beneficiaries/BEN_ERRO/schedule"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value(message))
                .andExpect(jsonPath("$.path")
                        .value("/beneficiaries/BEN_ERRO/schedule"));
    }

    @Test
    void shouldReturn409WhenBeneficiaryHasNoAuthorization() throws Exception {
        String message = "O beneficiário precisa registrar a autorização.";

        when(service.getSchedule("BEN001"))
                .thenThrow(new BeneficiaryNotAuthorizedException(message));

        mockMvc.perform(get("/beneficiaries/BEN001/schedule"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(message));
    }
}

