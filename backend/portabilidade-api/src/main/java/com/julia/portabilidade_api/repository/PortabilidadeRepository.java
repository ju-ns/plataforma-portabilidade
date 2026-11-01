package com.julia.portabilidade_api.repository;

import com.julia.portabilidade_api.entity.Portabilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PortabilidadeRepository extends JpaRepository<Portabilidade, UUID> {
}
