package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Pun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PunRepository extends JpaRepository<Pun, UUID> {
}
