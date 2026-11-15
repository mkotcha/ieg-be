package org.emmek.IEG.payloads;

import java.util.List;

/**
 * Confronto di tutte le fatture emesse per un mese di consumo con le fatture del fornitore.
 *
 * @param datiFornitore        ci sono righe del fornitore per il mese (altrimenti la bolletta non è ancora importata)
 * @param costiIeg             costi del fornitore verso IEG non legati a un cliente (prepayment, mora, bollo...)
 * @param podSenzaFattura      POD fatturati dal fornitore nel mese senza una nostra fattura
 */
public record ConfrontoMeseDTO(
        int mese,
        int anno,
        boolean datiFornitore,
        double totaleNostro,
        double totaleFornitore,
        double margine,
        double differenzaPassanti,
        double costiIeg,
        List<ConfrontoFatturaDTO> fatture,
        List<PodSenzaFatturaDTO> podSenzaFattura
) {
    /**
     * Pod con righe del fornitore nel mese ma senza una nostra fattura; cliente null se il pod non è in anagrafica.
     */
    public record PodSenzaFatturaDTO(String pod, boolean inAnagrafica, Long clienteId, String ragioneSociale,
                                     double importoFornitore) {
    }
}
