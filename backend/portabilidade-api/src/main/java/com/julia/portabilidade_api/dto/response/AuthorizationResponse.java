package com.julia.portabilidade_api.dto.response;


import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthorizationResponse {

    private UUID id;
    private String beneficiaryId;
    private String authorizationId;
    private PortabilityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;


    public AuthorizationResponse(Portability entity){
        this.id = entity.getId();
        this.beneficiaryId = entity.getBeneficiaryId();
        this.authorizationId = entity.getAuthorizationId();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.authorizedAt = entity.getAuthorizedAt();
    }
}
