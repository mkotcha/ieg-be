package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.AltraPartita;
import org.emmek.IEG.entities.Fornitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AltraPartitaRepository extends JpaRepository<AltraPartita, Long>, JpaSpecificationExecutor<AltraPartita> {
    List<AltraPartita> findByFornituraAndMeseAndAnnoOrderById(Fornitura fornitura, int mese, int anno);
}
