package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FatturaRepository extends JpaRepository<Fattura, Long> {
    List<Fattura> findByCliente(Cliente cliente);

    boolean existsByNumeroFattura(String numeroFattura);

    Fattura findByNumeroFattura(String numeroFattura);
}
