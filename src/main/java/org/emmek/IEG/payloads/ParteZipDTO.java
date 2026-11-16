package org.emmek.IEG.payloads;

/**
 * Una delle parti in cui sono divisi i pdf delle fatture di un mese per l'invio via email.
 *
 * @param parte    numero della parte, da 1
 * @param totale   numero di parti del mese
 * @param fatture  numero di pdf nella parte
 * @param dimensione    somma delle dimensioni dei pdf (lo zip pesa circa altrettanto)
 * @param nomeFile nome del file zip
 */
public record ParteZipDTO(int parte, int totale, int fatture, long dimensione, String nomeFile) {
}
