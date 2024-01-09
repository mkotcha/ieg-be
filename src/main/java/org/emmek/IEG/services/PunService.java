package org.emmek.IEG.services;

import org.emmek.IEG.entities.Pun;
import org.emmek.IEG.repositories.PunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PunService {

    @Autowired
    private PunRepository punRepository;

    public Pun getByMeseAndAnno(int mese, int anno) {
        return punRepository.findByMeseAndAnno(mese, anno).orElseThrow(() -> new RuntimeException("Pun non trovato mese: " + mese + " - anno: " + anno));
    }
}
