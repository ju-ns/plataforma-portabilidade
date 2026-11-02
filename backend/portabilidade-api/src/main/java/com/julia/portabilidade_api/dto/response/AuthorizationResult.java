package com.julia.portabilidade_api.dto.response;

import com.julia.portabilidade_api.entities.Portability;

public record AuthorizationResult(
        Portability portability,
        boolean created) {
}
