package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface FornituraRepository extends JpaRepository<Fornitura, String> {

    @NonNull
    Optional<Fornitura> findById(@NonNull String pod);

    @Query("SELECT id FROM Fornitura")
    List<String> getListaPod();
}
