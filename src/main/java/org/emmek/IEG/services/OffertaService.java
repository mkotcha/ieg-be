package org.emmek.IEG.services;

import org.emmek.IEG.entities.Offerta;
import org.emmek.IEG.repositories.OffertaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OffertaService {

    @Autowired
    private OffertaRepository offertaRepository;

    public Offerta findByNome(String nome) {
        return offertaRepository.findByNome(nome).orElseThrow(() -> new RuntimeException("Prezzo con nome: " + nome + " non trovato"));
    }

    public Offerta save(Offerta offerta) {
        return offertaRepository.save(offerta);
    }

    public Offerta findById(Long id) {
        return offertaRepository.findById(id).orElseThrow(() -> new RuntimeException("Prezzo con id: " + id + " non trovato"));

    }
}
