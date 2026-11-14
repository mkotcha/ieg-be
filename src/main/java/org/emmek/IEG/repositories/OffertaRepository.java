package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Offerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OffertaRepository extends JpaRepository<Offerta, Long> {

    Optional<Offerta> findByNome(String nome);

    @Query("select count(f) from Fornitura f where f.offerta.id = :id")
    long countForniture(@Param("id") Long id);

}
