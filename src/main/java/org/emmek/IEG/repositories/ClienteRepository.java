package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @NonNull
    public Optional<Cliente> findById(@NonNull long id);
}
