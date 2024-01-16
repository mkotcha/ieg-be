package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Programmazione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgrammazioneRepository extends JpaRepository<Programmazione, Long> {

    Optional<Programmazione> findByNome(String maggiorato);
}
