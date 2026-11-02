package com.julia.portabilidade_api.dto.response;

import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;

import java.util.UUID;

public record PortabilityResponse(
        UUID id,
        String beneficiaryId,
        String protocol,
        PortabilityStatus status,
        String rejectionReason
) {
    public PortabilityResponse(Portability portability) {
        this(
                portability.getId(),
                portability.getBeneficiaryId(),
                portability.getProtocolCentral(),
                portability.getStatus(),
                portability.getRejectionReason()
        );
    }
}
