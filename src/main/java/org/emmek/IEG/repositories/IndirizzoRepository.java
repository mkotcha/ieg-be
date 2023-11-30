package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Indirizzo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IndirizzoRepository extends JpaRepository<Indirizzo, UUID> {
}
