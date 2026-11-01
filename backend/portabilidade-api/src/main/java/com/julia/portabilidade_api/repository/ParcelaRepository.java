package com.julia.portabilidade_api.repository;

import com.julia.portabilidade_api.entity.Parcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ParcelaRepository extends JpaRepository <Parcela, UUID> {
}
