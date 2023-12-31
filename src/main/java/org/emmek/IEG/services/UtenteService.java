package org.emmek.IEG.services;

import jakarta.transaction.Transactional;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.repositories.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class UtenteService {
    @Autowired
    private UtenteRepository utenteRepository;

    public Page<Utente> getUsers(int page, int size, String orderBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(orderBy));
        return utenteRepository.findAll(pageable);
    }

    public Utente findByEmail(String email) {
        return utenteRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("utente con email" + email + "non trovato"));
    }

    public Utente findById(Long id) throws NotFoundException {
        return utenteRepository.findById(id).orElseThrow(() -> new NotFoundException(id.toString()));
    }

    public Utente findByIdAndUpdate(Long id, Utente body) throws NotFoundException {
        Utente found = this.findById(id);
        found.setCognome(body.getCognome());
        found.setNome(body.getNome());
        found.setEmail(body.getEmail());
        found.setUsername(body.getUsername());
        return utenteRepository.save(found);
    }

    public void findByIdAndDelete(Long id) throws NotFoundException {
        Utente found = this.findById(id);
        utenteRepository.delete(found);
    }

    public void save(Utente user) {
        utenteRepository.save(user);
    }

    @Transactional
    public Utente findByUsername(String username) {
        return utenteRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("utente con username " + username + " non trovato"));
    }
}
