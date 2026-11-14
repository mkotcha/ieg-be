package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.MappaturaVoceFornitore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MappaturaVoceFornitoreRepository extends JpaRepository<MappaturaVoceFornitore, Long> {
    List<MappaturaVoceFornitore> findAllByOrderByVoceConfrontoAscChiaveAsc();
}
