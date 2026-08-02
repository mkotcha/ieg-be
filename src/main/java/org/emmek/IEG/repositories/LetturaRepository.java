package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LetturaRepository extends JpaRepository<Lettura, Long> {

    @Query(value = "SELECT l FROM Lettura l " +
            "JOIN FETCH l.fornitura f " +
            "JOIN FETCH f.cliente " +
            "LEFT JOIN FETCH f.prezzo " +
            "LEFT JOIN FETCH f.programmazione",
            countQuery = "SELECT count(l) FROM Lettura l")
    Page<Lettura> findAllFetched(Pageable pageable);

    @Query("SELECT MAX(l.id) FROM Lettura l")
    Long findMaxId();


    List<Lettura> findByFornituraAndDataLetturaBetween(Fornitura fornitura, LocalDate from, LocalDate to);

    List<Lettura> findByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(Fornitura fornitura, LocalDate from, LocalDate to);

    int countByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(Fornitura fornitura, LocalDate from, LocalDate to);

    List<Lettura> findByFornituraAndDataLettura(Fornitura fornitura, LocalDate dataLettura);
}
