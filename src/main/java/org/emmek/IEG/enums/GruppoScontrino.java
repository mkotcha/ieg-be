package org.emmek.IEG.enums;

/**
 * Voci dello scontrino dell'energia (pagina 2) in cui vengono sommati gli importi del dettaglio.
 */
public enum GruppoScontrino {
    /** quota consumi: spesa per vendita energia elettrica */
    VENDITA,
    /** quota consumi: spesa per la rete e gli oneri generali di sistema */
    RETE,
    /** quota fissa: spesa per vendita energia elettrica */
    FISSA_VENDITA,
    /** quota fissa: spesa per la rete e gli oneri generali di sistema */
    FISSA_RETE,
    /** quota potenza: spesa per la rete e gli oneri generali di sistema */
    POTENZA_RETE
}
