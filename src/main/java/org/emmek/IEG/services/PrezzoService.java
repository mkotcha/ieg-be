package org.emmek.IEG.services;

import org.emmek.IEG.entities.Prezzo;
import org.emmek.IEG.repositories.PrezzoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrezzoService {

    @Autowired
    private PrezzoRepository prezzoRepository;

    public Prezzo findByNome(String nome) {
        return prezzoRepository.findByNome(nome).orElseThrow(() -> new RuntimeException("Prezzo con nome: " + nome + " non trovato"));
    }

    public Prezzo save(Prezzo prezzo) {
        return prezzoRepository.save(prezzo);
    }

    public Prezzo findById(Long id) {
        return prezzoRepository.findById(id).orElseThrow(() -> new RuntimeException("Prezzo con id: " + id + " non trovato"));

    }
}
