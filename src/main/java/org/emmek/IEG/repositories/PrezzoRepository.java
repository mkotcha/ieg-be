package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Prezzo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrezzoRepository extends JpaRepository<Prezzo, Long> {

    Optional<Prezzo> findByNome(String nome);

}
