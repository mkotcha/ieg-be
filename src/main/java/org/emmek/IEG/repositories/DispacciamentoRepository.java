package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Dispacciamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DispacciamentoRepository extends JpaRepository<Dispacciamento, UUID> {
}
