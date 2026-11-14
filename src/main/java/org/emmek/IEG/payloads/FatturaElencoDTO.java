package org.emmek.IEG.payloads;

import java.time.LocalDate;

/**
 * Riga dell'elenco delle fatture (GET /api/fatture/elenco), senza dettagli per fornitura.
 *
 * @param mese           mese di consumo
 * @param anno           anno di consumo
 * @param forniture      pod fatturati
 * @param totaleDaPagare somma dei totali da pagare delle forniture (null per le fatture generate prima del campo)
 */
public record FatturaElencoDTO(
        String numeroFattura,
        LocalDate dataFattura,
        int mese,
        int anno,
        Long clienteId,
        String ragioneSociale,
        Long forniture,
        Double consumoTot,
        Double totaleImponibile,
        Double totaleIva,
        Double totaleDaPagare
) {
}
