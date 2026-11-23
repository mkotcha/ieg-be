package org.emmek.IEG.payloads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record UtenteUpdateDTO(
        @NotEmpty(message = "L'username è un campo obbligatorio!")
        String username,
        String nome,
        String cognome,
        @NotEmpty(message = "L'email è un campo obbligatorio!")
        @Pattern(regexp = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$", message = "L'email inserita non è valida")
        String email,
        @NotEmpty(message = "Il ruolo è un campo obbligatorio!")
        @Pattern(regexp = "ADMIN|USER", message = "Il ruolo deve essere ADMIN o USER")
        String ruolo) {
}
