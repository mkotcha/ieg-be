package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.FatturaFornitore;
import org.emmek.IEG.entities.VoceFatturaFornitore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VoceFatturaFornitoreRepository extends JpaRepository<VoceFatturaFornitore, Long>,
        JpaSpecificationExecutor<VoceFatturaFornitore> {

    // righe di competenza del mese dei pod che non sono in anagrafica
    @Query("SELECT v FROM VoceFatturaFornitore v JOIN FETCH v.fatturaFornitore " +
            "WHERE v.mese = :mese AND v.anno = :anno AND v.pod IS NOT NULL AND v.pod NOT IN (SELECT f.id FROM Fornitura f)")
    List<VoceFatturaFornitore> findPodNonRegistrati(@Param("mese") int mese, @Param("anno") int anno);

    @Query("SELECT v FROM VoceFatturaFornitore v JOIN FETCH v.fatturaFornitore WHERE v.mese = :mese AND v.anno = :anno")
    List<VoceFatturaFornitore> findByCompetenza(@Param("mese") int mese, @Param("anno") int anno);

    @Query("SELECT v FROM VoceFatturaFornitore v JOIN FETCH v.fatturaFornitore " +
            "WHERE v.mese = :mese AND v.anno = :anno AND v.pod IN :pod")
    List<VoceFatturaFornitore> findByCompetenzaAndPod(@Param("mese") int mese, @Param("anno") int anno,
                                                      @Param("pod") Collection<String> pod);

    @Modifying
    @Query("DELETE FROM VoceFatturaFornitore v WHERE v.fatturaFornitore = :fattura")
    void deleteByFatturaFornitore(@Param("fattura") FatturaFornitore fattura);

    @Query("SELECT DISTINCT v.codTariffa, v.nrArticolo, v.descrizione FROM VoceFatturaFornitore v")
    List<Object[]> findTariffeDistinte();
}
