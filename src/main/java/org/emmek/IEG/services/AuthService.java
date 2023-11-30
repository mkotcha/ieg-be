package org.emmek.IEG.services;

import org.emmek.IEG.entities.Ruolo;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.exceptions.UnauthorizedException;
import org.emmek.IEG.payloads.NewUtenteDTO;
import org.emmek.IEG.payloads.UtenteLoginDTO;
import org.emmek.IEG.repositories.RuoloRepository;
import org.emmek.IEG.repositories.UtenteRepository;
import org.emmek.IEG.security.JWTTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AuthService {
    @Autowired
    private UtenteService utenteService;
    @Autowired
    private RuoloService ruoloService;
    @Autowired
    private JWTTools jwtTools;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private PasswordEncoder bcrypt;
    @Autowired
    private RuoloRepository ruoloRepository;

    public String autheticateUtente(UtenteLoginDTO body) {
        Utente utente = utenteService.findByUsername(body.username());
        if (bcrypt.matches(body.password(), utente.getPassword())) {
            return jwtTools.createToken(utente);
        } else {
            throw new UnauthorizedException("credenziali non valide");
        }
    }


    public Utente save(NewUtenteDTO body) throws IOException {
        utenteRepository.findByEmail(body.email()).ifPresent(utente -> {
            throw new BadRequestException("L'email " + utente.getEmail() + " è già utilizzata!");
        });
        utenteRepository.findByUsername(body.username()).ifPresent(utente -> {
            throw new BadRequestException("Lo username " + utente.getUsername() + " è già utilizzato!");
        });
        Utente newUtente = new Utente();
        newUtente.setNome(body.nome());
        newUtente.setCognome(body.cognome());
        newUtente.setEmail(body.email());
        newUtente.setPassword(bcrypt.encode(body.password()));
        newUtente.setUsername(body.username());
        Ruolo ruolo = ruoloService.findByRuolo("USER");
        newUtente.addRuolo(ruolo);

        return utenteRepository.save(newUtente);
    }
}

