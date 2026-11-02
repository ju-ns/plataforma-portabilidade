package com.julia.portabilidade_api.repositories;

import com.julia.portabilidade_api.entities.Portability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortabilityRepository extends JpaRepository<Portability, UUID> {

    Optional<Portability> findByIdempotencyKey(
            String idempotencyKey
    );
}
