package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Lettura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LetturaRepository extends JpaRepository<Lettura, UUID> {
}
