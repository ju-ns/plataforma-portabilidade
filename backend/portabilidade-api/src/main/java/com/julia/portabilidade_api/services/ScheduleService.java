package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.BeneficiaryNotAuthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;

    public List<InstallmentResponse> getSchedule(String beneficiaryId) {
        validateAuthorization(beneficiaryId);

        return centralClient.getSchedule(beneficiaryId);
    }

    private void validateAuthorization(String beneficiaryId) {
        boolean authorized =
                repository.existsByBeneficiaryIdAndAuthorizationIdIsNotNull(
                        beneficiaryId
                );

        if (!authorized) {
            throw new BeneficiaryNotAuthorizedException(
                    "O beneficiário precisa registrar a autorização "
                            + "antes de consultar a agenda."
            );
        }
    }
}
