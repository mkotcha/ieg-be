package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Oneri;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OneriRepository extends JpaRepository<Oneri, UUID> {
}
