package org.emmek.IEG.payloads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Cambio password: {@code passwordAttuale} è richiesta quando l'utente cambia la propria
 * ({@code PUT /api/utenti/me/password}), ignorata nel reset fatto da un ADMIN.
 */
public record CambioPasswordDTO(
        String passwordAttuale,
        @NotEmpty(message = "La nuova password è un campo obbligatorio!")
        @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
        String nuovaPassword) {
}
