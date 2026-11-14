package org.emmek.IEG.payloads;

import java.time.LocalDate;
import java.util.List;

/**
 * Letture di una fornitura per la fattura del mese, con lo stesso periodo che usa il calcolo della fattura
 * (LetturaService.periodoLetture).
 *
 * @param dal           inizio del periodo (lettura finale della fattura precedente, lettura di switch o fine mese
 *                      precedente, vedi origineInizio)
 * @param al            ultimo giorno del mese
 * @param letture       letture nel periodo, estremi inclusi
 * @param primaLettura  prima lettura del periodo: da qui parte davvero la fattura
 * @param ultimaLettura ultima lettura del periodo: qui finisce davvero la fattura
 * @param fatturabile   ci sono almeno due letture: la fattura si può calcolare (anche con problemi)
 * @param problemi      cosa manca o va controllato; vuota se il periodo è coperto per intero
 */
public record VerificaLettureDTO(
        String pod,
        Long clienteId,
        String ragioneSociale,
        LocalDate dal,
        LocalDate al,
        String origineInizio,
        int letture,
        LocalDate primaLettura,
        LocalDate ultimaLettura,
        String tipoUltimaLettura,
        boolean fatturabile,
        List<String> problemi
) {
}
