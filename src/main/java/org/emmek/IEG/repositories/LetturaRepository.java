package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LetturaRepository extends JpaRepository<Lettura, Long> {

    @Query("SELECT MAX(l.id) FROM Lettura l")
    Long findMaxId();


    List<Lettura> findByFornituraAndDataLetturaBetween(Fornitura fornitura, LocalDate from, LocalDate to);

    List<Lettura> findByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(Fornitura fornitura, LocalDate from, LocalDate to);
}
