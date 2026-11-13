package org.emmek.IEG.payloads;

/**
 * Esito della generazione della fattura di un cliente nella generazione massiva.
 *
 * @param totale imponibile + iva + altre partite; null se la generazione è fallita
 * @param errore messaggio d'errore; null se la fattura è stata generata
 */
public record EsitoFatturaDTO(Long clienteId, String ragioneSociale, String numeroFattura, Double totale,
                              long millisecondi, String errore) {
}
