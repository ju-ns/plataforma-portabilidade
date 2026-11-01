package com.julia.portabilidade_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity(name="tb_parcela")
public class Parcela {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String parcelaId;

    private String convenioId;

    private BigDecimal valor;

    private LocalDate dataDesconto;
    private String status;

    @ManyToOne
    @JoinColumn(name="portabilidade_id")
    private Portabilidade portabilidade;

}
