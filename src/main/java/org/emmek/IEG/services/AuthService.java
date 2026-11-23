package org.emmek.IEG.services;

import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.exceptions.UnauthorizedException;
import org.emmek.IEG.payloads.UtenteLoginDTO;
import org.emmek.IEG.payloads.UtenteLoginSuccessDTO;
import org.emmek.IEG.security.JWTTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UtenteService utenteService;
    @Autowired
    private JWTTools jwtTools;
    @Autowired
    private PasswordEncoder bcrypt;

    public UtenteLoginSuccessDTO autheticateUtente(UtenteLoginDTO body) {
        Utente utente;
        try {
            utente = utenteService.findByUsername(body.username());
        } catch (NotFoundException e) {
            // stesso messaggio della password sbagliata: non rivela quali username esistono
            throw new UnauthorizedException("credenziali non valide");
        }
        if (body.password() != null && bcrypt.matches(body.password(), utente.getPassword())) {
            return new UtenteLoginSuccessDTO(jwtTools.createToken(utente), utente.getRuolo());
        } else {
            throw new UnauthorizedException("credenziali non valide");
        }
    }
}
