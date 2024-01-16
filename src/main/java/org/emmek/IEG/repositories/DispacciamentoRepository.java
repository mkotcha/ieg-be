package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Dispacciamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispacciamentoRepository extends JpaRepository<Dispacciamento, Long> {
    public Optional<Dispacciamento> findByTrimestreAndAnno(int trimestre, Integer anno);
}
