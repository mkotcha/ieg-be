package org.emmek.IEG.payloads;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewUtenteDTO(
        @NotEmpty(message = "L'username è un campo obbligatorio!")
        String username,
        @NotEmpty(message = "La password è un campo obbligatorio!")
        @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
        String password,
        String nome,
        String cognome,
        @NotEmpty(message = "L'email è un campo obbligatorio!")
        @Pattern(regexp = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$", message = "L'email inserita non è valida")
        String email,
        @NotEmpty(message = "Il ruolo è un campo obbligatorio!")
        @Pattern(regexp = "ADMIN|USER", message = "Il ruolo deve essere ADMIN o USER")
        String ruolo) {
}
