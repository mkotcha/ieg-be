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
        assertEquals(33, fs.getVoci().size());
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
