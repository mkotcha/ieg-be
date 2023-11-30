package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    @NonNull
    public Optional<Cliente> findById(@NonNull UUID id);
}
