package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Pun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PunRepository extends JpaRepository<Pun, Long> {
    Optional<Pun> findByMeseAndAnno(int mese, int anno);
}
