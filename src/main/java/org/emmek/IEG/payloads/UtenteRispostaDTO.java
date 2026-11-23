package org.emmek.IEG.payloads;

import org.emmek.IEG.entities.Utente;

import java.util.Date;

public record UtenteRispostaDTO(Long id, String username, String nome, String cognome, String email, String ruolo,
                                Date createdAt) {
    public static UtenteRispostaDTO of(Utente u) {
        return new UtenteRispostaDTO(u.getId(), u.getUsername(), u.getNome(), u.getCognome(), u.getEmail(),
                u.getRuolo(), u.getCreatedAt());
    }
}
