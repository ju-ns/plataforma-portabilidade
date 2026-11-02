package com.julia.portabilidade_api.repositories;

import com.julia.portabilidade_api.entities.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository <Installment, UUID> {
}
