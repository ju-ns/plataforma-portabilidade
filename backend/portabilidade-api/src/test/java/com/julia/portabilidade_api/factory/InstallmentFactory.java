package com.julia.portabilidade_api.factory;

import com.julia.portabilidade_api.dto.response.InstallmentResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InstallmentFactory {

    public static InstallmentResponse installment(String installmentId,LocalDate deductionDate) {
        return new InstallmentResponse(
                installmentId,
                "CONV01",
                new BigDecimal("150.00"),
                deductionDate,
                "ABERTA"
        );
    }
}
