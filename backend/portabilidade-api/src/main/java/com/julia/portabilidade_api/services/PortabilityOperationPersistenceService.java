package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.entities.Installment;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.InstallmentRepository;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortabilityOperationPersistenceService {

    private final PortabilityRepository repository;
    private final InstallmentRepository installmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portability reserve(
            CreatePortabilityRequest request,
            String requestHash,
            String authorizationId
    ) {
        var portability = Portability.builder()
                .beneficiaryId(request.beneficiaryId())
                .authorizationId(authorizationId)
                .destinationBank(request.destinationBank())
                .destinationBranch(request.destinationBranch())
                .destinationAccount(request.destinationAccount())
                .idempotencyKey(request.idempotencyKey())
                .requestHash(requestHash)
                .createdAt(LocalDateTime.now())
                .status(PortabilityStatus.CRIADA)
                .build();

        repository.saveAndFlush(portability);

        var installments = request.installmentIds().stream()
                .map(installmentId -> {
                    var installment = new Installment();
                    installment.setInstallmentId(installmentId);
                    installment.setPortability(portability);
                    return installment;
                })
                .toList();

        installmentRepository.saveAll(installments);

        return portability;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portability markPending(UUID id, String protocol) {
        var portability = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFound(
                        "Operação de portabilidade não encontrada."
                ));

        portability.markPending(protocol);

        return portability;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUncertain(UUID id) {
        repository.updateStatusIfCurrent(
                id,
                PortabilityStatus.CRIADA,
                PortabilityStatus.PORTABILIDADE_INCERTA
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portability applyCentralStatus(
            UUID id,
            PortabilityStatus status,
            String rejectionReason) {
        if (status == PortabilityStatus.CONFIRMADA
                || status == PortabilityStatus.REJEITADA) {
            repository.completeIfPending(
                    id,
                    PortabilityStatus.PENDENTE,
                    status,
                    status == PortabilityStatus.REJEITADA
                            ? rejectionReason
                            : null
            );
        } else if (status != PortabilityStatus.PENDENTE) {
            throw new IllegalArgumentException(
                    "Status não suportado para acompanhamento."
            );
        }

        // Retorna o estado atual, inclusive se outra consulta
        // já tiver concluído a operação.
        return repository.findByIdAndRequestHashIsNotNull(id)
                .orElseThrow(() -> new ResourceNotFound(
                        "Operação de portabilidade não encontrada."
                ));

    }
}
