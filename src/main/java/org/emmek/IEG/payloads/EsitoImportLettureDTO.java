package org.emmek.IEG.payloads;

import java.util.List;

/**
 * Esito dell'import dei flussi di misura (risultato del lavoro IMPORT_LETTURE).
 *
 * @param file             file xml di misura trovati nell'archivio
 * @param fileScartati     file che non è stato possibile leggere
 * @param inserite         letture nuove
 * @param aggiornate       letture già presenti per lo stesso pod e giorno, sovrascritte
 * @param switchMantenute  letture periodiche ignorate perché lo stesso giorno c'è una lettura di switch
 * @param errori           misure non importate per dati non validi
 * @param podSconosciuti   pod presenti nei flussi ma non in anagrafica (misure ignorate)
 */
public record EsitoImportLettureDTO(
        int file,
        int fileScartati,
        int inserite,
        int aggiornate,
        int switchMantenute,
        int errori,
        List<String> podSconosciuti
) {
}
