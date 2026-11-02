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

    /*
    * reserve salva o registro com status CRIADA.
    * O saveAndFlush força a inserção para detectar uma chave duplicada,
    * e a transação confirma a reserva antes de retornar
    * */
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

    /*
    authorize busca o registro e atualiza os dados recebidos da Central.
    Como a entidade foi carregada dentro da transação,
    o JPA salva as alterações ao concluir o método, sem precisar chamar save.
     */
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
}
