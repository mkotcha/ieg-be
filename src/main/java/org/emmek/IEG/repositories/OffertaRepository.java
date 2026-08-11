package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Offerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OffertaRepository extends JpaRepository<Offerta, Long> {

    Optional<Offerta> findByNome(String nome);

}
