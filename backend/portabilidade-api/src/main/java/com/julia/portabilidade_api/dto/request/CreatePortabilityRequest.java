package com.julia.portabilidade_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePortabilityRequest(
        @NotBlank String beneficiaryId,

        @NotEmpty List<@NotBlank String> installmentIds,

        @NotBlank String destinationBank,
        @NotBlank String destinationBranch,
        @NotBlank String destinationAccount,

        @NotBlank @Size(max = 255) String idempotencyKey
) {
}
