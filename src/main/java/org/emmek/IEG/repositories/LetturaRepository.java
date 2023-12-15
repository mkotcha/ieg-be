package org.emmek.IEG.repositories;

import org.emmek.IEG.entities.Lettura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LetturaRepository extends JpaRepository<Lettura, Long> {

    @Query("SELECT MAX(l.id) FROM Lettura l")
    Long findMaxId();


}
