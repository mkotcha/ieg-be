package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.FatturaFornitore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FatturaFornitoreRepository extends JpaRepository<FatturaFornitore, Long> {
    Optional<FatturaFornitore> findByNumero(String numero);

    List<FatturaFornitore> findAllByOrderByDataEmissioneDesc();
}
