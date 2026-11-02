package com.julia.portabilidade_api.services;

import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.PortabilityUncertainException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortabilityStatusService {

    private final PortabilityRepository repository;
    private final CentralClient centralClient;
    private final PortabilityOperationPersistenceService persistenceService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Portability getStatus(UUID id) {
        var portability = repository.findByIdAndRequestHashIsNotNull(id)
                .orElseThrow(() -> new ResourceNotFound(
                        "Operação de portabilidade não encontrada."
                ));

        if (portability.getStatus() != PortabilityStatus.PENDENTE) {
            return portability;
        }

        String protocol = portability.getProtocolCentral();

        if (protocol == null || protocol.isBlank()) {
            throw new PortabilityUncertainException(
                    "A operação não possui protocolo para consulta na Central."
            );
        }

        var response = centralClient.getPortabilityStatus(protocol);

        return persistenceService.applyCentralStatus(
                id,
                response.status(),
                response.rejectionReason()
        );
    }
}
