package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.PunPod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PunPodRepository extends JpaRepository<PunPod, Long> {
    Optional<PunPod> findByFornituraAndMeseAndAnno(Fornitura fornitura, int mese, int anno);
}
