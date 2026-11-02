package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.BeneficiaryNotAuthorizedException;
import com.julia.portabilidade_api.services.exceptions.InvalidInstallmentSelectionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
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

    public List<InstallmentResponse> getSelectedInstallments(
            String beneficiaryId,
            List<String> installmentIds
    ) {
        var selectedIds = new HashSet<>(installmentIds);

        if (selectedIds.size() != installmentIds.size()) {
            throw new InvalidInstallmentSelectionException(
                    "A seleção contém parcelas repetidas."
            );
        }

        var selected = getSchedule(beneficiaryId).stream()
                .filter(item -> selectedIds.contains(item.installmentId()))
                .toList();

        if (selected.size() != selectedIds.size()) {
            throw new InvalidInstallmentSelectionException(
                    "Uma ou mais parcelas não pertencem à agenda do beneficiário."
            );
        }

        LocalDate today = LocalDate.now();

        boolean hasIneligibleInstallment = selected.stream()
                .anyMatch(item ->
                        !"ABERTA".equals(item.status())
                                || item.deductionDate() == null
                                || !item.deductionDate().isAfter(today)
                );

        if (hasIneligibleInstallment) {
            throw new InvalidInstallmentSelectionException(
                    "Selecione apenas parcelas abertas com data de desconto futura."
            );
        }

        return selected;
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
