package org.emmek.IEG.payloads;

import org.emmek.IEG.enums.VoceConfronto;

/**
 * Confronto di una voce tra la nostra fattura e quella del fornitore. Le quantità sono null dove non hanno senso
 * (quote fisse, altre partite). differenze = nostro - fornitore.
 *
 * @param anomala per le voci passanti differenza d'importo oltre la tolleranza; per energia e perdite differenza
 *                di quantità oltre 1 kWh (il fornitore arrotonda i kWh all'unità)
 */
public record ConfrontoVoceDTO(
        VoceConfronto voce,
        String descrizione,
        boolean passante,
        Double quantitaNostra,
        Double importoNostro,
        Double quantitaFornitore,
        Double importoFornitore,
        Double differenzaQuantita,
        Double differenzaImporto,
        boolean anomala
) {
}
