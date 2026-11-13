package org.emmek.IEG.services;

import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.BTA;
import org.emmek.IEG.enums.GruppoScontrino;
import org.emmek.IEG.enums.ModalitaProgrammazione;
import org.emmek.IEG.enums.SezioneFattura;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static org.emmek.IEG.enums.GruppoScontrino.*;
import static org.emmek.IEG.enums.SezioneFattura.*;

/**
 * Calcola tutte le voci di una fattura singola (dettaglio, totali e scontrino).
 * Le voci riproducono le righe del template pagina_3: ogni voce ha codice = nome della cella voce_[codice].
 * I consumi (consumoF1..F3, perdite, reattiva) devono essere già impostati sulla fattura singola.
 */
@Service
public class CalcoloFatturaService {

    private static final String EURO = "€";
    private static final String KWH = EURO + "/kWh";
    private static final String PDP = EURO + "/PdP";
    private static final String KW = EURO + "/kW";
    private static final String KVARH = EURO + "/kVARh";

    // prezzi della reattiva: non sono in tabella oneri, valori presi dal template
    private static final double PREZZO_REATTIVA_33 = 0.01088;
    private static final double PREZZO_REATTIVA_75 = 0.01088;
    // addizionale erariale se non indicata in tabella oneri
    private static final double ACCISE_DEFAULT = 0.0125;

    public void calcola(FatturaSingola fs, Pun pun) {
        calcola(fs, pun, List.of());
    }

    /**
     * @param pun          PUN da applicare (serve solo per le offerte a PUN)
     * @param altrePartite importi del mese già comprensivi di iva: fuori dall'imponibile, solo nel totale da pagare
     */
    public void calcola(FatturaSingola fs, Pun pun, List<AltraPartita> altrePartite) {
        Fornitura fornitura = fs.getFornitura();
        Offerta offerta = fornitura.getOfferta();
        if (offerta == null) throw new RuntimeException("Fornitura " + fornitura.getId() + " senza offerta");
        Oneri oneri = fs.getOneri();
        Dispacciamento disp = fs.getDispacciamento();
        LocalDate dal = fs.getLetturaIniziale().getDataLettura();
        LocalDate al = fs.getLetturaFinale().getDataLettura();
        double iva = fornitura.getIva() / 100;

        // prezzi: PUN oppure prezzo fisso dell'offerta
        if (offerta.isPun()) {
            fs.setPrezzoF1(pun.getF1());
            fs.setPrezzoF2(pun.getF2());
            fs.setPrezzoF3(pun.getF3());
        } else {
            fs.setPrezzoF1(nvl(offerta.getF1()));
            fs.setPrezzoF2(nvl(offerta.getF2()));
            fs.setPrezzoF3(nvl(offerta.getF3()));
        }
        // potenza per le quote potenza: impegnata se sotto i 16 kW, altrimenti il picco prelevato
        double potenzaImpegnata = nvl(fornitura.getPotenzaImpegnata());
        fs.setPotenzaOneri(potenzaImpegnata < 16 ? potenzaImpegnata : nvl(fs.getPotenzaPrelevata()));

        double consumo = nvl(fs.getConsumoTot());
        double perdite = nvl(fs.getPerditeF1()) + nvl(fs.getPerditeF2()) + nvl(fs.getPerditeF3());
        double consumoConPerdite = consumo + perdite;
        double potenza = fs.getPotenzaOneri();
        double dispacciamento = nvl(disp.getDis()) + nvl(disp.getCapacita()) + nvl(disp.getSbilanciamento());
        double maggiorazione = offerta.isCapacitaMaggiorazione() ? nvl(offerta.getMaggiorazione()) : 0;
        double accise = oneri.getAccise() != null ? oneri.getAccise() : ACCISE_DEFAULT;
        boolean reattiva = fornitura.getBta() == BTA.BTA6;

        Voci v = new Voci(fs, dal, al, iva);
        // ---- spesa per la materia energia
        v.add("aggregazione", "Aggregazione misure contatori no orari", MATERIA, FISSA_VENDITA, PDP, nvl(disp.getCostoAm()), 1);
        v.add("energia_f1", "Quota Energia F1", MATERIA, VENDITA, KWH, fs.getPrezzoF1(), nvl(fs.getConsumoF1()));
        v.add("perdite_f1", "Perdite Quota Energia F1", MATERIA, VENDITA, KWH, fs.getPrezzoF1(), nvl(fs.getPerditeF1()));
        v.add("energia_f2", "Quota Energia F2", MATERIA, VENDITA, KWH, fs.getPrezzoF2(), nvl(fs.getConsumoF2()));
        v.add("perdite_f2", "Perdite Quota Energia F2", MATERIA, VENDITA, KWH, fs.getPrezzoF2(), nvl(fs.getPerditeF2()));
        v.add("energia_f3", "Quota Energia F3", MATERIA, VENDITA, KWH, fs.getPrezzoF3(), nvl(fs.getConsumoF3()));
        v.add("perdite_f3", "Perdite Quota Energia F3", MATERIA, VENDITA, KWH, fs.getPrezzoF3(), nvl(fs.getPerditeF3()));
        v.add("spread", "Spread su P.U.N.", MATERIA, VENDITA, KWH, offerta.getSpreadEffettivo(), consumoConPerdite);
        v.add("dispacciamento", "Dispacciamento", MATERIA, VENDITA, KWH, dispacciamento, consumo);
        v.add("perdite_dispacciamento", "Perdite Dispacciamento", MATERIA, VENDITA, KWH, dispacciamento, perdite);
        v.add("commercializzazione", "Prezzo commercializzazione vendita", MATERIA, FISSA_VENDITA, PDP, nvl(offerta.getOcf()), 1);
        if (offerta.getModalitaProgrammazione() == ModalitaProgrammazione.CLASSIC) {
            // percentuale sulla spesa per energia: quote energia F1-F3, relative perdite e spread
            double spesaEnergia = v.importo("energia_f1", "perdite_f1", "energia_f2", "perdite_f2", "energia_f3", "perdite_f3", "spread");
            v.add("ocv", offerta.getDescrizione() != null ? offerta.getDescrizione() : "Oneri di programmazione",
                    MATERIA, VENDITA, PDP, nvl(offerta.getOcv()) * spesaEnergia / 100, 1);
        } else {
            v.add("ocv", offerta.getDescrizione() != null ? offerta.getDescrizione() : "Oneri di commercializzazione variabili",
                    MATERIA, VENDITA, KWH, nvl(offerta.getOcv()), consumoConPerdite);
        }
        v.add("rettifica", "Rettifica oneri di programmazione", MATERIA, null, PDP, 0, 1);
        // ---- trasporto e gestione del contatore
        v.add("maggiorazione_capacita", "Maggiorazione Corrispettivo Mercato Capacità var", TRASPORTO, VENDITA, KWH, maggiorazione, consumo);
        v.add("perdite_maggiorazione_capacita", "Perdite Maggiorazione Corrispettivo Mercato Capacità var", TRASPORTO, VENDITA, KWH, maggiorazione, perdite);
        v.add("qe_tud", "Quota Energia - Distribuzione", TRASPORTO, RETE, KWH, nvl(oneri.getQeTud()), consumo);
        v.add("qe_uc3", "Quota Energia - Oneri di Sistema UC3", TRASPORTO, RETE, KWH, nvl(oneri.getQeUc3()), consumo);
        v.add("qe_uc6", "Quota Energia - Oneri di Sistema UC6", TRASPORTO, RETE, KWH, nvl(oneri.getQeUc6()), consumo);
        v.add("trasmissione", "Trasmissione Energia", TRASPORTO, RETE, KWH, nvl(oneri.getTrasmissione()), consumo);
        v.add("reattiva_33", "Energia Reattiva immessa tra 33% e 75%", TRASPORTO, RETE, KVARH, PREZZO_REATTIVA_33, reattiva ? reattiva33(fs) : 0);
        v.add("reattiva_75", "Energia Reattiva immessa oltre 75%", TRASPORTO, RETE, KVARH, PREZZO_REATTIVA_75, reattiva ? reattiva75(fs) : 0);
        v.add("qf_tud", "Quota Fissa - Distribuzione", TRASPORTO, FISSA_RETE, PDP, nvl(oneri.getQfTud()), 1);
        v.add("qf_mis", "Quota Fissa - Misura", TRASPORTO, FISSA_VENDITA, PDP, nvl(oneri.getQfMis()), 1);
        v.add("qf_uc6", "Quota Fissa - Oneri di Sistema UC6", TRASPORTO, FISSA_RETE, PDP, nvl(oneri.getQfUc6()), 1);
        v.add("qf_dbt", "Quota Fissa - Dispacciamento BT", TRASPORTO, null, PDP, nvl(oneri.getQfDbt()), 1);
        v.add("qp_tdm", "Quota Potenza", TRASPORTO, POTENZA_RETE, KW, nvl(oneri.getQpTdm()), potenza);
        // ---- oneri di sistema
        v.add("qe_asos", "Quota Energia - Oneri di sistema ASOS", ONERI, RETE, KWH, nvl(oneri.getQeAsos()), consumo);
        v.add("qf_asos", "Quota Fissa - Oneri di sistema ASOS", ONERI, FISSA_RETE, PDP, nvl(oneri.getQfAsos()), 1);
        v.add("qp_asos", "Quota Potenza - Oneri di sistema ASOS", ONERI, POTENZA_RETE, KW, nvl(oneri.getQpAsos()), potenza);
        v.add("qe_arim", "Quota Energia - Oneri di sistema ARIM", ONERI, RETE, KWH, nvl(oneri.getQeArim()), consumoConPerdite);
        v.add("qf_arim", "Quota Fissa - Oneri di sistema ARIM", ONERI, FISSA_RETE, PDP, nvl(oneri.getQfArim()), 1);
        v.add("qp_arim", "Quota Potenza - Oneri di sistema ARIM", ONERI, POTENZA_RETE, KW, nvl(oneri.getQpArim()), potenza);
        // ---- imposte
        v.add("addizionale_erariale", "Addizionale Erariale", IMPOSTE, null, KWH, accise, consumo);
        // ---- altre partite: già comprensive di iva
        int n = 0;
        for (AltraPartita partita : altrePartite) {
            v.add("altra_partita_" + (++n), partita.getVoce(), ALTRE_PARTITE, null, EURO, nvl(partita.getImporto()), 1);
            fs.getVoci().get(fs.getVoci().size() - 1).setIva(0.0);
        }

        // ---- totali
        fs.setTotaleMateria(v.totale(MATERIA));
        fs.setTotaleTrasporto(v.totale(TRASPORTO));
        fs.setTotaleOneri(v.totale(ONERI));
        fs.setTotaleImposte(v.totale(IMPOSTE));
        fs.setTotalePartite(v.totale(ALTRE_PARTITE));
        // le altre partite sono già comprensive di iva: escluse dall'imponibile e dal calcolo dell'iva
        fs.setTotaleImponibile(fs.getTotaleMateria() + fs.getTotaleTrasporto() + fs.getTotaleOneri() + fs.getTotaleImposte());
        fs.setTotaleIva(fs.getTotaleImponibile() * iva);

        // ---- scontrino
        fs.setSpesaVendita(v.totale(VENDITA));
        fs.setSpesaRete(v.totale(RETE));
        fs.setSpesaFissaVendita(v.totale(FISSA_VENDITA));
        fs.setSpesaFissaRete(v.totale(FISSA_RETE));
        fs.setSpesaPotenzaRete(v.totale(POTENZA_RETE));
        fs.setSpesaMedia(consumo > 0 ? (fs.getSpesaVendita() + fs.getSpesaRete()) / consumo : null);
        fs.setSpesaVenditaMedia(consumo > 0 ? fs.getSpesaVendita() / consumo : null);
        fs.setSpesaReteMedia(consumo > 0 ? fs.getSpesaRete() / consumo : null);
        fs.setSpesaPotenzaReteMedia(potenza > 0 ? fs.getSpesaPotenzaRete() / potenza : null);
    }

    /**
     * Raccoglie le voci sulla fattura singola mantenendo l'ordine del template.
     */
    private static class Voci {
        private final FatturaSingola fs;
        private final LocalDate dal;
        private final LocalDate al;
        private final double iva;

        Voci(FatturaSingola fs, LocalDate dal, LocalDate al, double iva) {
            this.fs = fs;
            this.dal = dal;
            this.al = al;
            this.iva = iva;
            fs.getVoci().clear();
        }

        void add(String codice, String descrizione, SezioneFattura sezione, GruppoScontrino gruppo,
                 String unita, double unitario, double quantita) {
            VoceFatturaSingola voce = new VoceFatturaSingola();
            voce.setFatturaSingola(fs);
            voce.setCodice(codice);
            voce.setDescrizione(descrizione);
            voce.setSezione(sezione);
            voce.setGruppoScontrino(gruppo);
            voce.setDal(dal);
            voce.setAl(al);
            voce.setUnita(unita);
            voce.setUnitario(unitario);
            voce.setQuantita(quantita);
            voce.setImporto(unitario * quantita);
            voce.setIva(iva);
            voce.setOrdine(fs.getVoci().size() + 1);
            fs.getVoci().add(voce);
        }

        double importo(String... codici) {
            List<String> lista = List.of(codici);
            return somma(fs.getVoci().stream().filter(v -> lista.contains(v.getCodice())).toList());
        }

        double totale(SezioneFattura sezione) {
            return somma(fs.getVoci().stream().filter(v -> v.getSezione() == sezione).toList());
        }

        double totale(GruppoScontrino gruppo) {
            return somma(fs.getVoci().stream().filter(v -> v.getGruppoScontrino() == gruppo).toList());
        }

        private static double somma(List<VoceFatturaSingola> voci) {
            return voci.stream().mapToDouble(VoceFatturaSingola::getImporto).sum();
        }
    }

    /**
     * Reattiva tra 33% e 75% dell'attiva.
     */
    static double reattiva33(FatturaSingola fs) {
        double attiva = nvl(fs.getConsumoTot());
        double reattiva = nvl(fs.getConsumoTotR());
        double percentuale = reattiva > 0 && attiva > 0 ? reattiva * 100 / attiva : 0;
        return percentuale > 33 ? reattiva - (reattiva / 100 * 33) - reattiva75(fs) : 0;
    }

    /**
     * Reattiva oltre il 75% dell'attiva.
     */
    static double reattiva75(FatturaSingola fs) {
        double attiva = nvl(fs.getConsumoTot());
        double reattiva = nvl(fs.getConsumoTotR());
        double percentuale = reattiva > 0 && attiva > 0 ? reattiva * 100 / attiva : 0;
        return percentuale > 75 ? reattiva - (reattiva / 100 * 75) : 0;
    }

    private static double nvl(Double valore) {
        return valore != null ? valore : 0;
    }
}
