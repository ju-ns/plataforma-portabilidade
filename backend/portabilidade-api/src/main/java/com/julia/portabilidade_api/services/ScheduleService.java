package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.BeneficiaryNotAuthorizedException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;

    public List<InstallmentResponse> getSchedule(String beneficiaryId) {
        validateAuthorization(beneficiaryId);

        var installments = centralClient.getSchedule(beneficiaryId);

        if (installments.isEmpty()) {
            throw new ResourceNotFound(
                    "Nenhum agendamento de desconto encontrado para o beneficiário "
                            + beneficiaryId + "."
            );
        }

        return installments.stream()
                .sorted(Comparator.comparing(
                        InstallmentResponse::deductionDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
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
