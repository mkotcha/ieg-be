package org.emmek.IEG.payloads;

import java.util.List;

/**
 * Confronto di una nostra fattura (cliente, mese) con le righe del fornitore delle sue forniture.
 * forniture è null nel riepilogo del mese (si chiede il dettaglio con /api/fatture/{numero}/confronto).
 *
 * @param differenzaPassanti somma delle differenze sulle voci passanti: dovrebbe essere ~0
 * @param anomalie           numero di anomalie delle forniture
 */
public record ConfrontoFatturaDTO(
        String numeroFattura,
        Long clienteId,
        String ragioneSociale,
        int mese,
        int anno,
        double totaleNostro,
        double totaleFornitore,
        double margine,
        double differenzaPassanti,
        int anomalie,
        List<ConfrontoFornituraDTO> forniture
) {
}
