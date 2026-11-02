package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;

import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.AuthorizationResult;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.AuthorizationUncertainException;
import com.julia.portabilidade_api.services.exceptions.DuplicateException;
import com.julia.portabilidade_api.services.exceptions.InvalidCentralResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;




@Service
@RequiredArgsConstructor
public class PortabilityService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;
    private final AuthorizationPersistenceService persistenceService;


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorizationResult registerAuthorization(
            RegisterAuthorizationRequest request
    ) {
        var existing = repository.findByIdempotencyKey(
                request.idempotencyKey()
        );

        if (existing.isPresent()) {
            return reuse(existing.get(), request);
        }

        return createAuthorization(request);
    }

    private AuthorizationResult createAuthorization(
            RegisterAuthorizationRequest request
    ) {
        Portability reserved;

        try {
            reserved = persistenceService.reserve(
                    request.beneficiaryId(),
                    request.idempotencyKey()
            );

        } catch (DataIntegrityViolationException exception) {
            var existing = repository.findByIdempotencyKey(
                    request.idempotencyKey()
            ).orElseThrow(() -> exception);

            return reuse(existing, request);
        }

        return authorizeReserved(reserved, request);
    }

    private AuthorizationResult authorizeReserved(
            Portability reserved,
            RegisterAuthorizationRequest request
    ) {
        try {
            String authorizationId = requestAuthorizationId(request);

            var authorized = persistenceService.authorize(
                    reserved.getId(),
                    authorizationId
            );

            return new AuthorizationResult(authorized, true);

        } catch (RestClientException
                 | InvalidCentralResponseException
                 | DataAccessException exception) {

            throw handleUncertainAuthorization(reserved, exception);
        }
    }

    private String requestAuthorizationId(
            RegisterAuthorizationRequest request
    ) {
        var response = centralClient.registerAuthorization(request);

        if (response == null
                || response.authorizationId() == null
                || response.authorizationId().isBlank()) {
            throw new InvalidCentralResponseException(
                    "A Central retornou uma autorização inválida."
            );
        }

        return response.authorizationId();
    }

    private AuthorizationUncertainException handleUncertainAuthorization(
            Portability reserved,
            RuntimeException cause
    ) {
        try {
            persistenceService.markUncertain(reserved.getId());

        } catch (DataAccessException persistenceException) {
            cause.addSuppressed(persistenceException);
        }

        return new AuthorizationUncertainException(
                "Não foi possível confirmar a autorização. "
                        + "Não envie uma nova chave para esta solicitação; "
                        + "o resultado precisa ser verificado com a Central.",
                cause
        );
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

        if (existing.getStatus() == PortabilityStatus.AUTORIZACAO_INCERTA) {
            throw new AuthorizationUncertainException(
                    "Esta solicitação tem resultado incerto. "
                            + "É necessário verificar a autorização com a Central "
                            + "antes de tentar novamente."
            );
        }

        return new AuthorizationResult(existing, false);
    }

}
