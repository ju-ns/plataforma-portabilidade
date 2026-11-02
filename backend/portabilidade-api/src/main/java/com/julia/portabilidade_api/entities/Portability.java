package com.julia.portabilidade_api.entities;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity(name="tb_portability")
public class Portability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String beneficiaryId;
    private String authorizationId;
    private String protocolCentral;
    private String destinationBank;
    private String destinationBranch;
    private String destinationAccount;

    @Enumerated(EnumType.STRING)
    private PortabilityStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;

    private LocalDateTime authorizedAt;



    @OneToMany (mappedBy = "portability")
    private List<Installment> installments;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    public void authorize(String authorizationId){
        this.authorizationId = authorizationId;
        this.status = PortabilityStatus.AUTORIZADA;
        this.authorizedAt = LocalDateTime.now();
    }


}
