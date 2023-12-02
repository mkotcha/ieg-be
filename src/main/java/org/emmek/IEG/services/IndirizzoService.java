package org.emmek.IEG.services;

import org.emmek.IEG.entities.Indirizzo;
import org.emmek.IEG.repositories.IndirizzoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IndirizzoService {

    @Autowired
    private IndirizzoRepository indirizzoRepository;

    public void save(Indirizzo indirizzo) {
        indirizzoRepository.save(indirizzo);
    }
}
