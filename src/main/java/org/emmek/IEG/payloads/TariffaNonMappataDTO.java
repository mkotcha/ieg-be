package org.emmek.IEG.payloads;

/**
 * Tariffa presente nelle fatture del fornitore che nessuna mappatura riconduce a una voce di confronto.
 */
public record TariffaNonMappataDTO(String codTariffa, String nrArticolo, String descrizione) {
}
