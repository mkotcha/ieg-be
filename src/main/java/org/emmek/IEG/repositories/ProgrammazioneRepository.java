package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Programmazione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProgrammazioneRepository extends JpaRepository<Programmazione, UUID> {
}
