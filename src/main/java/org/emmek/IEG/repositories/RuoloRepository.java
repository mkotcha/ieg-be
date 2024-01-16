package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Ruolo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RuoloRepository extends JpaRepository<Ruolo, Long> {

    public Optional<Ruolo> findByRuolo(String ruolo);
}
