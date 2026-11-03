package com.julia.portabilidade_api.services;


import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationPersistenceService {

    private final PortabilityRepository repository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portability reserve(
            String beneficiaryId,
            String idempotencyKey
    ){
        var portability = Portability.builder()
                .beneficiaryId(beneficiaryId)
                .idempotencyKey(idempotencyKey)
                .status(PortabilityStatus.CRIADA)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.saveAndFlush(portability);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Portability authorize(
            UUID portabilityId,
            String authorizationId
    ){
        var portability = repository.findById(portabilityId)
                .orElseThrow(() -> new
                        ResourceNotFound("Registro da autorização não encontrado."
                ));
        portability.authorize(authorizationId);
        return portability;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUncertain(UUID portabilityId){
        repository.updateStatusIfCurrent(
                portabilityId,
                PortabilityStatus.CRIADA,
                PortabilityStatus.AUTORIZACAO_INCERTA
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markAbandonedAsUncertain(LocalDateTime cutoff) {
        return repository.updateAbandonedAuthorizations(
                PortabilityStatus.CRIADA,
                PortabilityStatus.AUTORIZACAO_INCERTA,
                cutoff
        );
    }
}
