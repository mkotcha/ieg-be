package org.emmek.IEG.services;

import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.CambioPasswordDTO;
import org.emmek.IEG.payloads.NewUtenteDTO;
import org.emmek.IEG.payloads.UtenteUpdateDTO;
import org.emmek.IEG.repositories.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestione utenti. Due ruoli: ADMIN (tutto) e USER (sola lettura, vedi SecurityConfig). Gli utenti li crea solo un
 * ADMIN; non si può togliere il ruolo ADMIN all'ultimo ADMIN, né eliminare sé stessi.
 */
@Service
public class UtenteService {
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private RuoloService ruoloService;
    @Autowired
    private PasswordEncoder bcrypt;

    public List<Utente> findAll() {
        return utenteRepository.findAll(Sort.by("username"));
    }

    public Utente findById(Long id) throws NotFoundException {
        return utenteRepository.findById(id).orElseThrow(() -> new NotFoundException("utente " + id + " non trovato"));
    }

    @Transactional
    public Utente findByUsername(String username) {
        return utenteRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("utente con username " + username + " non trovato"));
    }

    public void save(Utente user) {
        utenteRepository.save(user);
    }

    @Transactional
    public Utente crea(NewUtenteDTO body) {
        verificaUnicita(null, body.username(), body.email());
        Utente utente = new Utente();
        utente.setUsername(body.username().trim());
        utente.setNome(body.nome());
        utente.setCognome(body.cognome());
        utente.setEmail(body.email().trim());
        utente.setPassword(bcrypt.encode(body.password()));
        utente.setRuolo(ruoloService.findByRuolo(body.ruolo()));
        return utenteRepository.save(utente);
    }

    @Transactional
    public Utente aggiorna(Long id, UtenteUpdateDTO body, Utente corrente) {
        Utente utente = findById(id);
        verificaUnicita(id, body.username(), body.email());
        if ("ADMIN".equals(utente.getRuolo()) && !"ADMIN".equals(body.ruolo())) {
            if (utente.getId().equals(corrente.getId())) {
                throw new BadRequestException("Non puoi toglierti il ruolo ADMIN da solo");
            }
            verificaNonUltimoAdmin();
        }
        utente.setUsername(body.username().trim());
        utente.setNome(body.nome());
        utente.setCognome(body.cognome());
        utente.setEmail(body.email().trim());
        utente.setRuolo(ruoloService.findByRuolo(body.ruolo()));
        return utenteRepository.save(utente);
    }

    /** Cambio della propria password: serve quella attuale. */
    @Transactional
    public void cambiaPassword(Utente corrente, CambioPasswordDTO body) {
        Utente utente = findById(corrente.getId());
        if (body.passwordAttuale() == null || !bcrypt.matches(body.passwordAttuale(), utente.getPassword())) {
            throw new BadRequestException("La password attuale non è corretta");
        }
        utente.setPassword(bcrypt.encode(body.nuovaPassword()));
        utenteRepository.save(utente);
    }

    /** Reset della password di un utente da parte di un ADMIN. */
    @Transactional
    public void resetPassword(Long id, CambioPasswordDTO body) {
        Utente utente = findById(id);
        utente.setPassword(bcrypt.encode(body.nuovaPassword()));
        utenteRepository.save(utente);
    }

    @Transactional
    public void elimina(Long id, Utente corrente) {
        Utente utente = findById(id);
        if (utente.getId().equals(corrente.getId())) {
            throw new BadRequestException("Non puoi eliminare il tuo utente");
        }
        if ("ADMIN".equals(utente.getRuolo())) {
            verificaNonUltimoAdmin();
        }
        utenteRepository.delete(utente);
    }

    private void verificaNonUltimoAdmin() {
        if (utenteRepository.countByRuoli_Ruolo("ADMIN") <= 1) {
            throw new BadRequestException("Deve restare almeno un utente ADMIN");
        }
    }

    private void verificaUnicita(Long id, String username, String email) {
        utenteRepository.findByUsername(username.trim()).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
            throw new BadRequestException("Lo username " + u.getUsername() + " è già utilizzato!");
        });
        utenteRepository.findByEmail(email.trim()).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
            throw new BadRequestException("L'email " + u.getEmail() + " è già utilizzata!");
        });
    }
}
