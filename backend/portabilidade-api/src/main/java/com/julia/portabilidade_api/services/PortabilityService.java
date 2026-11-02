package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;

import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.AuthorizationResult;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import com.julia.portabilidade_api.services.exceptions.InvalidCentralResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortabilityService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;
    private final AuthorizationPersistenceService persistenceService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorizationResult registerAuthorization(RegisterAuthorizationRequest request){
        var existing = repository.findByIdempotencyKey(
                request.idempotencyKey());

        if (existing.isPresent()) {
            return reuse(existing.get(), request);
        }
        Portability reserved;
        try{
            reserved = persistenceService.reserve(
                    request.beneficiaryId(),
                    request.idempotencyKey()
            );
        } catch(DataIntegrityViolationException exception){
            var concurrent = repository.findByIdempotencyKey(
                    request.idempotencyKey()
            );
            if(concurrent.isEmpty()){
                throw exception;
            }
            return reuse(concurrent.get(), request);
        }

        var response = centralClient.registerAuthorization(request);

        if (response == null
                || response.authorizationId() == null
                || response.authorizationId().isBlank()) {
            throw new InvalidCentralResponseException(
                    "A Central retornou uma autorização inválida."
            );
        }

        var authorized = persistenceService.authorize(
                reserved.getId(),
                response.authorizationId());
        return new AuthorizationResult(authorized, true);
    }

    private AuthorizationResult reuse(
            Portability existing,
            RegisterAuthorizationRequest request
    ) {
        if (!existing.getBeneficiaryId().equals(request.beneficiaryId())) {
            throw new DuplicateException(
                    "Esta chave de idempotência já foi usada para outro beneficiário."
            );
        }

        // Esta chamada recuperou um registro existente.
        return new AuthorizationResult(existing, false);
    }

}
