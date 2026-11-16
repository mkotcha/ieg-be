package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LetturaRepository extends JpaRepository<Lettura, Long> {

    @Query("SELECT MAX(l.id) FROM Lettura l")
    Long findMaxId();


    List<Lettura> findByFornituraAndDataLetturaBetween(Fornitura fornitura, LocalDate from, LocalDate to);

    // solo le letture attive: quelle disattivate non entrano nei calcoli
    List<Lettura> findByFornituraAndAttivaTrueAndDataLetturaBetweenOrderByDataLetturaDesc(Fornitura fornitura, LocalDate from, LocalDate to);


    List<Lettura> findByFornituraAndDataLettura(Fornitura fornitura, LocalDate dataLettura);

    // letture (anche disattivate) della fornitura nelle date indicate, dalla più recente
    List<Lettura> findByFornituraAndDataLetturaInOrderByDataLetturaDescIdDesc(Fornitura fornitura, Collection<LocalDate> date);

    // letture con la costante ka valorizzata più vicine a una data: l'ultima fino alla data e la prima dopo
    Optional<Lettura> findFirstByFornituraAndKaGreaterThanAndDataLetturaLessThanEqualOrderByDataLetturaDesc(Fornitura fornitura, Double ka, LocalDate data);

    Optional<Lettura> findFirstByFornituraAndKaGreaterThanAndDataLetturaGreaterThanOrderByDataLetturaAsc(Fornitura fornitura, Double ka, LocalDate data);

    // lettura di switch: Raccolta = "S" nei flussi SNM/SNM2G
    Optional<Lettura> findFirstByFornituraAndAttivaTrueAndRaccoltaAndDataLetturaBetweenOrderByDataLetturaDesc(Fornitura fornitura, String raccolta, LocalDate from, LocalDate to);
}
