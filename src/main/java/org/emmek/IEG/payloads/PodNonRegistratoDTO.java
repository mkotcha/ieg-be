package org.emmek.IEG.payloads;

import java.time.LocalDate;
import java.util.List;

/**
 * Pod presente nelle righe del fornitore di competenza di un mese ma non in anagrafica, con i dati per proporne la
 * registrazione presi dalla riga più recente del mese. I campi che il riepilogo non permette di ricavare sono null.
 *
 * @param bta                classe utenza del fornitore, se è un valore di BTA
 * @param tipoPrelievo       BT/MT/AT dal tipo utenza (es. "ALTRI USI BT RE")
 * @param codiceDistributore dal codice pod (IT001 = e-distribuzione)
 * @param dataApertura       apertura dell'utenza presso il fornitore: proposta come data di switch
 * @param importoFornitore   totale delle righe del fornitore per il pod nel mese
 * @param bollette           bollette del fornitore in cui compare
 */
public record PodNonRegistratoDTO(
        String pod,
        String bta,
        Double potenzaImpegnata,
        Double potenzaDisponibile,
        String tipoPrelievo,
        String codiceDistributore,
        LocalDate dataApertura,
        int ultimoMese,
        int ultimoAnno,
        double importoFornitore,
        List<String> bollette
) {
}
