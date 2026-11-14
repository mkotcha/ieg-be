package org.emmek.IEG.payloads;

import java.time.LocalDate;

/**
 * Consumo attivo di una fornitura in un mese di calendario per fascia (kWh, costanti del contatore applicate,
 * senza perdite), calcolato dalle letture.
 *
 * @param dal      lettura di partenza: l'ultima fino a fine mese precedente
 * @param al       lettura di arrivo: l'ultima fino a fine mese
 * @param completo dal e al sono proprio fine mese precedente e fine mese: il consumo copre tutto il mese (per un
 *                 cliente: per tutti i suoi pod)
 * @param forniture      pod con consumo nel mese (1 per una fornitura)
 * @param fornitureTotali pod considerati (per un cliente tutti i suoi; 1 per una fornitura)
 */
public record ConsumoMensileDTO(
        int mese,
        int anno,
        double f1,
        double f2,
        double f3,
        double totale,
        LocalDate dal,
        LocalDate al,
        boolean completo,
        int forniture,
        int fornitureTotali
) {
}
