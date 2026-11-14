package org.emmek.IEG.services;

import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.BTA;
import org.emmek.IEG.enums.ModalitaProgrammazione;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Confronta il calcolo con la fattura di esempio AUEE202611007 (CALYPSO, POD IT001E04318295, ottobre 2026)
 * generata dalle macro VBA.
 */
class CalcoloFatturaServiceTest {

    private static final double DELTA = 0.0001;

    @Test
    void calcolaComeFatturaDiEsempio() {
        FatturaSingola fs = fatturaDiEsempio();
        new CalcoloFatturaService().calcola(fs, punDiEsempio());

        // pagina 3: totali
        assertEquals(16.9683, fs.getTotaleMateria(), DELTA);
        assertEquals(3.7292, fs.getTotaleTrasporto(), DELTA);
        assertEquals(2.4453, fs.getTotaleOneri(), DELTA);
        assertEquals(0.2948, fs.getTotaleImposte(), DELTA);
        assertEquals(23.4376, fs.getTotaleImponibile(), DELTA);
        assertEquals(5.1563, fs.getTotaleIva(), DELTA);
        // pagina 2: scontrino
        assertEquals(0.208, fs.getPotenzaOneri(), DELTA);
        assertEquals(7.3505, fs.getSpesaVendita(), DELTA);
        assertEquals(1.1566, fs.getSpesaRete(), DELTA);
        assertEquals(11.8276, fs.getSpesaFissaVendita(), DELTA);
        assertEquals(1.9208, fs.getSpesaFissaRete(), DELTA);
        assertEquals(0.8874, fs.getSpesaPotenzaRete(), DELTA);
        // nell'esempio la media era calcolata da importi arrotondati a 4 decimali (8,5071 / 23,584)
        assertEquals(0.360715, fs.getSpesaMedia(), 0.00001);
        assertEquals(34, fs.getVoci().size());
    }

    @Test
    void oneriProgrammazioneClassicInPercentualeSullaSpesaEnergia() {
        FatturaSingola fs = fatturaDiEsempio();
        fs.getFornitura().getOfferta().setModalitaProgrammazione(ModalitaProgrammazione.CLASSIC);
        fs.getFornitura().getOfferta().setOcv(30.0);
        new CalcoloFatturaService().calcola(fs, punDiEsempio());

        double spesaEnergia = fs.getVoci().stream()
                .filter(v -> List.of("energia_f1", "perdite_f1", "energia_f2", "perdite_f2", "energia_f3", "perdite_f3", "spread").contains(v.getCodice()))
                .mapToDouble(VoceFatturaSingola::getImporto).sum();
        VoceFatturaSingola ocv = fs.getVoci().stream().filter(v -> v.getCodice().equals("ocv")).findFirst().orElseThrow();
        assertEquals(6.4367, spesaEnergia, DELTA);
        assertEquals(spesaEnergia * 0.30, ocv.getImporto(), DELTA);
        assertEquals(1.0, ocv.getQuantita());
    }

    @Test
    void reattivaImmessaSoloF3PerBta6() {
        FatturaSingola fs = fatturaDiEsempio();
        // immessa di CALYPSO a ottobre 2026: il fornitore fattura solo la F3 (27,556 -> 28 kVArh)
        fs.setConsumoF1rc(20.997);
        fs.setConsumoF2rc(16.537);
        fs.setConsumoF3rc(27.556);
        fs.setConsumoTotRc(65.09);
        new CalcoloFatturaService().calcola(fs, punDiEsempio());

        VoceFatturaSingola voce = fs.getVoci().stream().filter(v -> v.getCodice().equals("reattiva_immessa")).findFirst().orElseThrow();
        assertEquals(0.01088, voce.getUnitario());
        assertEquals(27.556, voce.getQuantita());
        assertEquals(0.2998, voce.getImporto(), DELTA);
        assertEquals(3.7292 + 0.2998, fs.getTotaleTrasporto(), DELTA);
        assertEquals(1.1566 + 0.2998, fs.getSpesaRete(), DELTA);
    }

    @Test
    void reattivaImmessaNonFatturataSeNonBta6() {
        FatturaSingola fs = fatturaDiEsempio();
        fs.getFornitura().setBta(BTA.BTA3);
        fs.setConsumoF3rc(27.556);
        new CalcoloFatturaService().calcola(fs, punDiEsempio());

        VoceFatturaSingola voce = fs.getVoci().stream().filter(v -> v.getCodice().equals("reattiva_immessa")).findFirst().orElseThrow();
        assertEquals(0.0, voce.getImporto());
    }

    @Test
    void reattivaInduttivaPerFasciaSoloF1F2() {
        FatturaSingola fs = fatturaDiEsempio();
        // GLG SRLS ottobre 2026 (circa): il fornitore fattura 34 kVArh tra 33% e 75%
        fs.setConsumoF1(678.0);
        fs.setConsumoF1r(218.0); // 32%: niente penale
        fs.setConsumoF2(368.0);
        fs.setConsumoF2r(155.0); // 155 - 33% di 368 = 33,56
        fs.setConsumoF3(437.0);
        fs.setConsumoF3r(277.0); // F3 esclusa
        new CalcoloFatturaService().calcola(fs, punDiEsempio());
        assertEquals(33.56, voce(fs, "reattiva_33").getQuantita(), DELTA);
        assertEquals(0.0, voce(fs, "reattiva_75").getQuantita(), DELTA);

        // oltre il 75%: la parte tra 33% e 75% va in reattiva_33, il resto in reattiva_75
        fs.setConsumoF1(100.0);
        fs.setConsumoF1r(90.0);
        fs.setConsumoF2(0.0);
        fs.setConsumoF2r(0.0);
        new CalcoloFatturaService().calcola(fs, punDiEsempio());
        assertEquals(42.0, voce(fs, "reattiva_33").getQuantita(), DELTA);
        assertEquals(15.0, voce(fs, "reattiva_75").getQuantita(), DELTA);
    }

    @Test
    void altrePartiteSoggetteAIvaNellImponibile() {
        FatturaSingola fs = fatturaDiEsempio();
        AltraPartita pratica = new AltraPartita();
        pratica.setVoce("Onere per pratica di aumento di potenza");
        pratica.setImporto(75.29);
        pratica.setEsenteIva(false);
        AltraPartita morosita = new AltraPartita();
        morosita.setVoce("Corrispettivo Morosità Energia");
        morosita.setImporto(332.28); // esente iva: default
        new CalcoloFatturaService().calcola(fs, punDiEsempio(), List.of(pratica, morosita));

        assertEquals(75.29 + 332.28, fs.getTotalePartite(), DELTA);
        assertEquals(23.4376 + 75.29, fs.getTotaleImponibile(), DELTA);
        assertEquals((23.4376 + 75.29) * 0.22, fs.getTotaleIva(), DELTA);
        assertEquals((23.4376 + 75.29) * 1.22 + 332.28, fs.getTotaleDaPagare(), DELTA);
        assertEquals(0.22, voce(fs, "altra_partita_1").getIva());
        assertEquals(0.0, voce(fs, "altra_partita_2").getIva());
    }

    private static VoceFatturaSingola voce(FatturaSingola fs, String codice) {
        return fs.getVoci().stream().filter(v -> v.getCodice().equals(codice)).findFirst().orElseThrow();
    }

    @Test
    void altrePartiteIvaInclusaFuoriDallImponibile() {
        FatturaSingola fs = fatturaDiEsempio();
        AltraPartita partita = new AltraPartita();
        partita.setVoce("Corrispettivo Morosità Energia");
        partita.setImporto(332.28);
        new CalcoloFatturaService().calcola(fs, punDiEsempio(), List.of(partita));

        assertEquals(332.28, fs.getTotalePartite(), DELTA);
        // imponibile e iva come senza partite
        assertEquals(23.4376, fs.getTotaleImponibile(), DELTA);
        assertEquals(5.1563, fs.getTotaleIva(), DELTA);
        VoceFatturaSingola voce = fs.getVoci().get(fs.getVoci().size() - 1);
        assertEquals("Corrispettivo Morosità Energia", voce.getDescrizione());
        assertEquals(0.0, voce.getIva());
    }

    private static FatturaSingola fatturaDiEsempio() {
        Offerta offerta = new Offerta();
        offerta.setPun(true);
        offerta.setHasSpread(true);
        offerta.setSpread(0.012);
        offerta.setOcf(10.0);
        offerta.setOcv(0.005);
        offerta.setCapacitaMaggiorazione(true);
        offerta.setMaggiorazione(0.015);

        Fornitura fornitura = new Fornitura();
        fornitura.setId("IT001E04318295");
        fornitura.setOfferta(offerta);
        fornitura.setIva(22.0);
        fornitura.setPotenzaImpegnata(28.0);
        fornitura.setBta(BTA.BTA6);

        Dispacciamento disp = new Dispacciamento();
        disp.setCostoAm(0.007);
        disp.setDis(0.0098);
        disp.setCapacita(0.003423);
        disp.setSbilanciamento(0.002);

        Oneri oneri = new Oneri();
        oneri.setQeTud(0.00066);
        oneri.setQeUc3(0.00276);
        oneri.setQeUc6(0.00007);
        oneri.setTrasmissione(0.0119);
        oneri.setQfTud(0.445592);
        oneri.setQfMis(1.820575);
        oneri.setQfUc6(0.1402);
        oneri.setQfDbt(0.0);
        oneri.setQpTdm(2.744142);
        oneri.setQeAsos(0.031875);
        oneri.setQfAsos(1.0827);
        oneri.setQpAsos(1.2347);
        oneri.setQeArim(0.001614);
        oneri.setQfArim(0.2523);
        oneri.setQpArim(0.2877);
        oneri.setAccise(0.0125);

        Lettura iniziale = new Lettura();
        iniziale.setDataLettura(LocalDate.of(2026, 9, 30));
        Lettura finale = new Lettura();
        finale.setDataLettura(LocalDate.of(2026, 10, 31));

        FatturaSingola fs = new FatturaSingola();
        fs.setFornitura(fornitura);
        fs.setOneri(oneri);
        fs.setDispacciamento(disp);
        fs.setLetturaIniziale(iniziale);
        fs.setLetturaFinale(finale);
        fs.setConsumoF1(7.215);
        fs.setConsumoF2(6.061);
        fs.setConsumoF3(10.308);
        fs.setPerditeF1(0.7215);
        fs.setPerditeF2(0.6061);
        fs.setPerditeF3(1.0308);
        fs.setConsumoTot(23.584);
        fs.setConsumoTotR(0.307);
        fs.setPotenzaPrelevata(0.208);

        return fs;
    }

    private static Pun punDiEsempio() {
        Pun pun = new Pun();
        pun.setF1(0.238375);
        pun.setF2(0.258289);
        pun.setF3(0.221494);

        return pun;
    }
}
