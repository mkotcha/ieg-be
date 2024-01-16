package org.emmek.IEG.services;

import org.emmek.IEG.entities.Programmazione;
import org.emmek.IEG.repositories.ProgrammazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgrammazioneService {

    @Autowired
    private ProgrammazioneRepository programmazioneRepository;

    public Programmazione findByNome(String maggiorato) {
        return programmazioneRepository.findByNome(maggiorato).orElseThrow(() -> new RuntimeException("Programmazione con nome: " + maggiorato + " non trovata"));
    }

    public Programmazione save(Programmazione programmazione) {
        return programmazioneRepository.save(programmazione);
    }
}
