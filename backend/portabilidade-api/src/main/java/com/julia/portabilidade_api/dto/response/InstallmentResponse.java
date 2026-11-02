package com.julia.portabilidade_api.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        @JsonAlias("parcelaId")
        String installmentId,

        @JsonAlias("convenioId")
        String agreementId,

        @JsonAlias("valor")
        BigDecimal amount,

        @JsonAlias("dataDesconto")
        LocalDate deductionDate,

        String status


) {
}
