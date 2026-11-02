package com.julia.portabilidade_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAuthorizationRequest(
        @NotBlank(message = "O beneficiário é obrigatório.")
        String beneficiaryId,

        @NotBlank(message = "A chave de idempotência é obrigatória.")
        @Size(max = 255, message = "A chave deve ter até 255 caracteres.")
        String idempotencyKey


) {
}
