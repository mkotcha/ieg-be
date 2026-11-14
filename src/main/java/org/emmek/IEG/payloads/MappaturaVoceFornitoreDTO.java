package org.emmek.IEG.payloads;

import jakarta.validation.constraints.NotBlank;
import org.emmek.IEG.enums.ValueOfEnum;
import org.emmek.IEG.enums.VoceConfronto;

public record MappaturaVoceFornitoreDTO(
        // cod tariffa (o nr articolo se la tariffa è vuota); con '*' finale vale come prefisso
        @NotBlank(message = "chiave obbligatoria")
        String chiave,
        boolean perdite,
        @NotBlank(message = "voceConfronto obbligatoria")
        @ValueOfEnum(enumClass = VoceConfronto.class, message = "voceConfronto non valida")
        String voceConfronto,
        boolean quantita
) {
}
