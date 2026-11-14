package org.emmek.IEG.payloads;

import java.time.LocalDate;
import java.util.List;

/**
 * Esito dell'import di una bolletta del fornitore.
 *
 * @param sostituita     la bolletta era già stata importata e le sue righe sono state sostituite
 * @param righeScartate  righe del csv non leggibili (vedi log)
 * @param podSconosciuti POD del csv che non sono in anagrafica forniture
 * @param righeNonMappate righe che nessuna mappatura riconduce a una voce di confronto
 */
public record EsitoImportFatturaFornitoreDTO(
        String numero,
        LocalDate dataEmissione,
        String nomeFile,
        boolean sostituita,
        int righe,
        int righeScartate,
        double totaleImponibile,
        List<String> podSconosciuti,
        int righeNonMappate
) {
}
