package org.emmek.IEG.enums;

public enum StatoLavoro {
    IN_CORSO,
    COMPLETATO,
    // errore che ha interrotto il lavoro (gli errori sui singoli elementi non lo interrompono)
    FALLITO
}
