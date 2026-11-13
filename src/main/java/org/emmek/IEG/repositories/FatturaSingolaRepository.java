package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.FatturaSingola;
import org.emmek.IEG.entities.Fornitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface FatturaSingolaRepository extends JpaRepository<FatturaSingola, Long> {

    @Query("SELECT MAX(fs.letturaFinale.dataLettura) FROM FatturaSingola fs " +
            "WHERE fs.fornitura = :fornitura AND fs.fattura.mese = :mese AND fs.fattura.anno = :anno")
    Optional<LocalDate> findDataUltimaLetturaFatturata(@Param("fornitura") Fornitura fornitura,
                                                       @Param("mese") int mese,
                                                       @Param("anno") int anno);

}
