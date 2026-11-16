package org.emmek.IEG.enums;

import lombok.Getter;

/**
 * Voci comuni su cui si confrontano le nostre fatture con le righe della fattura del fornitore.
 * Passante: costo che giriamo al cliente così com'è, quindi nostro importo e del fornitore devono coincidere;
 * sulle altre (energia, commercializzazione, maggiorazioni capacità e sbilanciamento) la differenza è il margine.
 */
@Getter
public enum VoceConfronto {
    ENERGIA_F1("Energia F1", false),
    ENERGIA_F2("Energia F2", false),
    ENERGIA_F3("Energia F3", false),
    PERDITE_F1("Perdite F1", false),
    PERDITE_F2("Perdite F2", false),
    PERDITE_F3("Perdite F3", false),
    COMMERCIALIZZAZIONE("Commercializzazione e spread", false),
    DISPACCIAMENTO("Dispacciamento", true),
    // nostra maggiorazione sul corrispettivo capacità: il fornitore non ce l'ha, è margine
    MAGGIORAZIONE_CAPACITA("Maggiorazione capacità", false),
    // nostra maggiorazione sullo sbilanciamento (offerte con maggiorazione_sbilanciamento): è margine
    MAGGIORAZIONE_SBILANCIAMENTO("Maggiorazione sbilanciamento", false),
    QUOTE_FISSE_DISPACCIAMENTO("Quote fisse dispacciamento", true),
    DISTRIBUZIONE("Quota energia distribuzione e misura", true),
    TRASMISSIONE("Trasmissione", true),
    QUOTE_FISSE_RETE("Quote fisse distribuzione e misura", true),
    QUOTE_POTENZA_RETE("Quote potenza distribuzione e trasmissione", true),
    UC3("UC3", true),
    UC6("UC6", true),
    ASOS_ENERGIA("ASOS quota energia", true),
    ASOS_FISSA("ASOS quota fissa", true),
    ASOS_POTENZA("ASOS quota potenza", true),
    ARIM_ENERGIA("ARIM quota energia", true),
    ARIM_FISSA("ARIM quota fissa", true),
    ARIM_POTENZA("ARIM quota potenza", true),
    REATTIVA_33("Reattiva tra 33% e 75%", true),
    REATTIVA_75("Reattiva oltre 75%", true),
    REATTIVA_IMMESSA("Reattiva immessa", true),
    // IEG versa le accise direttamente: il fornitore le fattura a 0, quindi né passanti né margine
    ACCISE("Accise", false),
    ALTRE_PARTITE("Altre partite", false),
    // costi del fornitore verso IEG non legati a un cliente (prepayment, mora, bollo, REMIT): fuori dal confronto
    // per fornitura, solo nel riepilogo del mese
    COSTI_IEG("Costi IEG", false),
    NON_MAPPATA("Non mappata", false);

    private final String descrizione;
    private final boolean passante;

    VoceConfronto(String descrizione, boolean passante) {
        this.descrizione = descrizione;
        this.passante = passante;
    }

    /**
     * Voce che entra nel margine (nostro - fornitore) della fornitura.
     */
    public boolean concorreAlMargine() {
        return this != ALTRE_PARTITE && this != COSTI_IEG && this != NON_MAPPATA && this != ACCISE;
    }
}
