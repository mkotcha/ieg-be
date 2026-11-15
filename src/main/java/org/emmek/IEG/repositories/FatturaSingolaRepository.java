package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.FatturaSingola;
import org.emmek.IEG.entities.Fornitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FatturaSingolaRepository extends JpaRepository<FatturaSingola, Long> {

    // date delle letture iniziali e finali delle fatture della fornitura
    @Query("SELECT fs.letturaIniziale.dataLettura FROM FatturaSingola fs WHERE fs.fornitura = :fornitura")
    List<LocalDate> findDateLettureIniziali(@Param("fornitura") Fornitura fornitura);

    @Query("SELECT fs.letturaFinale.dataLettura FROM FatturaSingola fs WHERE fs.fornitura = :fornitura")
    List<LocalDate> findDateLettureFinali(@Param("fornitura") Fornitura fornitura);

    @Query("SELECT MAX(fs.letturaFinale.dataLettura) FROM FatturaSingola fs " +
            "WHERE fs.fornitura = :fornitura AND fs.fattura.mese = :mese AND fs.fattura.anno = :anno")
    Optional<LocalDate> findDataUltimaLetturaFatturata(@Param("fornitura") Fornitura fornitura,
                                                       @Param("mese") int mese,
                                                       @Param("anno") int anno);

}
