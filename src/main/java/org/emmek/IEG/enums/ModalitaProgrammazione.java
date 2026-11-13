package org.emmek.IEG.enums;

/**
 * Come si calcolano gli oneri di programmazione / commercializzazione variabili dell'offerta (campo ocv).
 */
public enum ModalitaProgrammazione {
    /** ocv in €/kWh applicato al consumo con perdite */
    OCV,
    /** ocv in percentuale sulla spesa per energia (quote energia F1-F3, perdite e spread) */
    CLASSIC
}
