package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Oneri;
import org.emmek.IEG.enums.BTA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OneriRepository extends JpaRepository<Oneri, Long> {
    public Optional<Oneri> findByTipoAndTrimestreAndAnno(BTA bta, int trimestre, int anno);
}
