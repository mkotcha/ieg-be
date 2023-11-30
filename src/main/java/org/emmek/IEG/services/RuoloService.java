package org.emmek.IEG.services;

import org.emmek.IEG.entities.Ruolo;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.repositories.RuoloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RuoloService {

    @Autowired
    private RuoloRepository ruoloRepository;

    public Ruolo findByRuolo(String nome) {
        return ruoloRepository.findByRuolo(nome).orElseThrow(() -> new NotFoundException("Ruolo non trovato"));
    }

    public void save(Ruolo ruolo) {
        ruoloRepository.save(ruolo);
    }
}
