package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FornituraRepository extends JpaRepository<Fornitura, UUID> {
}
