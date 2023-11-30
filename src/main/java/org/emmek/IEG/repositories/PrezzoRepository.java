package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Prezzo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrezzoRepository extends JpaRepository<Prezzo, UUID> {
}
