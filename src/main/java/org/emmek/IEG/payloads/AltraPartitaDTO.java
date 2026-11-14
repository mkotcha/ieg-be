package org.emmek.IEG.payloads;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AltraPartitaDTO(
        @NotBlank(message = "pod obbligatorio")
        String pod,
        @NotBlank(message = "voce obbligatoria")
        String voce,
        @NotNull(message = "importo obbligatorio")
        Double importo,
        // se non indicato vale true (importo già comprensivo di iva o fuori campo)
        Boolean esenteIva,
        @Min(value = 1, message = "mese tra 1 e 12")
        @Max(value = 12, message = "mese tra 1 e 12")
        int mese,
        @Min(value = 2000, message = "anno non valido")
        int anno
) {
}
