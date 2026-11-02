package com.julia.portabilidade_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity(name="tb_portabilidade")
public class Portabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String beneficiarioId;
    private String autorizacaoId;
    private String protocoloCentral;
    private String bancoDestino;
    private String agenciaDestino;
    private String contaDestino;

    @Enumerated(EnumType.STRING)
    private StatusPortabilidade status;
    private String motivoRejeicao;
    private LocalDateTime criadaEm;

    private LocalDateTime autorizadaEm;



    @OneToMany (mappedBy = "portabilidade")
    private List<Parcela> parcelas;



}
