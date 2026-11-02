package com.julia.portabilidade_api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterAuthorizationResponse(
        @JsonProperty("autorizacaoId")
        String authorizationId
) {
}
