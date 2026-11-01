package com.julia.portabilidade_api.entity;

import jakarta.persistence.*;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity(name="tb_portabilidade")
public class Portabilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String beneficiarioId;
    private String autorizacaoId;
    private String protocolo;
    private String bancoDestino;
    private String agenciaDestino;
    private String contaDestino;

    @Enumerated(EnumType.STRING)
    private StatusPortabilidade status;
    private String motivoRejeicao;
    private LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;

    @OneToMany (mappedBy = "portabilidade")
    private List<Parcela> parcelas;


}
