package com.julia.portabilidade_api.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity(name="tb_parcela")
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String installmentId;

    private String agreementId;

    private BigDecimal amount;

    private LocalDate deductionDate;
    private String status;

    @ManyToOne
    @JoinColumn(name="portability_id")
    private Portability portability;

}
