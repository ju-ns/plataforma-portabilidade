package com.julia.portabilidade_api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.julia.portabilidade_api.entities.PortabilityStatus;

public record CentralPortabilityStatusResponse(
        PortabilityStatus status,

        @JsonProperty("motivoRejeicao")
        String rejectionReason
) {
}
