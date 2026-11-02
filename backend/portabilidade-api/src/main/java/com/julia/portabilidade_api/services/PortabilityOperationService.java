package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PortabilityOperationService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;
    private final PortabilityOperationPersistenceService persistenceService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Portability create(CreatePortabilityRequest request) {
        String hash = calculateHash(request);

        var existing = repository.findByIdempotencyKey(
                request.idempotencyKey()
        );

        return existing.map(
                portability -> reuse(portability, hash))
                .orElseGet(() -> createNew(request, hash));

    }

    private Portability createNew(
            CreatePortabilityRequest request,
            String hash
    ) {
        var authorization = repository
                .findFirstByBeneficiaryIdAndAuthorizedAtIsNotNullOrderByAuthorizedAtDesc(
                        request.beneficiaryId()
                )
                .orElseThrow(() -> new BeneficiaryNotAuthorizedException(
                        "O beneficiário precisa registrar a autorização."
                ));

        Portability reserved;

        try {
            reserved = persistenceService.reserve(
                    request,
                    hash,
                    authorization.getAuthorizationId()
            );

        } catch (DataIntegrityViolationException exception) {
            var concurrent = repository.findByIdempotencyKey(
                    request.idempotencyKey()
            ).orElseThrow(() -> exception);

            return reuse(concurrent, hash);
        }

        return submit(reserved, request);
    }
    private Portability submit(
            Portability reserved,
            CreatePortabilityRequest request
    ) {
        try {
            var response = centralClient.createPortability(
                    request,
                    reserved.getAuthorizationId()
            );

            return persistenceService.markPending(
                    reserved.getId(),
                    response.protocol()
            );

        } catch (HttpServerErrorException.ServiceUnavailable exception) {
            var uncertain = handleUncertainOperation(reserved, exception);

            throw new CentralUnavailableException(
                    "A Central está indisponível no momento.",
                    uncertain
            );

        } catch (RestClientException
                 | InvalidCentralResponseException
                 | DataAccessException exception) {

            throw handleUncertainOperation(reserved, exception);
        }
    }

    private PortabilityUncertainException handleUncertainOperation(
            Portability reserved,
            RuntimeException cause
    ) {
        try {
            persistenceService.markUncertain(reserved.getId());
        } catch (DataAccessException persistenceException) {
            cause.addSuppressed(persistenceException);
        }

        return new PortabilityUncertainException(
                "Não foi possível confirmar o envio da portabilidade. "
                        + "Não envie outra chave para esta solicitação; "
                        + "o resultado precisa ser verificado com a Central.",
                cause
        );
    }

    private Portability reuse(Portability existing, String hash) {
        if (!hash.equals(existing.getRequestHash())) {
            throw new DuplicateException(
                    "Esta chave já foi utilizada com outra solicitação."
            );
        }

        if (existing.getStatus() == PortabilityStatus.PORTABILIDADE_INCERTA) {
            throw new PortabilityUncertainException(
                    "Esta operação tem resultado incerto "
                            + "e precisa ser verificada com a Central."
            );
        }

        return existing;
    }

    private String calculateHash(CreatePortabilityRequest request) {
        String installmentIds = request.installmentIds().stream()
                .sorted()
                .map(this::encode)
                .collect(Collectors.joining());

        String content = Stream.of(
                        request.beneficiaryId(),
                        request.destinationBank(),
                        request.destinationBranch(),
                        request.destinationAccount(),
                        installmentIds
                )
                .map(this::encode)
                .collect(Collectors.joining());

        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Não foi possível calcular o hash da solicitação.",
                    exception
            );
        }
    }


    private String encode(String value) {
        return value.length() + ":" + value;
    }


}
