package com.julia.portabilidade_api.repositories;

import com.julia.portabilidade_api.entities.Portability;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortabilityRepository extends JpaRepository<Portability, UUID> {

    Optional<Portability> findByIdempotencyKey(
            String idempotencyKey
    );


    @Modifying
    @Query("""
        update tb_portability p
        set p.status = :targetStatus
        where p.id = :id
          and p.status = :currentStatus
        """)
    int updateStatusIfCurrent(
            @Param("id") UUID id,
            @Param("currentStatus") PortabilityStatus currentStatus,
            @Param("targetStatus") PortabilityStatus targetStatus
    );

    @Modifying
    @Query("""
        update tb_portability p
        set p.status = :targetStatus
        where p.status = :currentStatus
          and p.createdAt < :cutoff
        """)
    int updateAbandonedAuthorizations(
            @Param("currentStatus") PortabilityStatus currentStatus,
            @Param("targetStatus") PortabilityStatus targetStatus,
            @Param("cutoff") LocalDateTime cutoff
    );
}
