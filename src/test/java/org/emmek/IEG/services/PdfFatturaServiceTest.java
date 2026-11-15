package org.emmek.IEG.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.TipoLettura;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Genera il pdf della fattura di esempio (senza Spring né DB) e ne controlla pagine e contenuto.
 * Il pdf resta in target/fattura-di-esempio.pdf per controllarne l'aspetto.
 */
class PdfFatturaServiceTest {

    @Test
    void pdfConRiepilogoEDuePaginePerFornitura() throws IOException {
        Fattura fattura = fatturaDiEsempio(2);
        byte[] pdf = new PdfFatturaService().pdf(fattura);
        Files.write(Path.of("target", "fattura-di-esempio.pdf"), pdf);

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertEquals(5, documento.getNumberOfPages());
            String testo = new PDFTextStripper().getText(documento);
            // pagina 1
            assertTrue(testo.contains("AUEE202611007"));
            assertTrue(testo.contains("Fattura di Ottobre 2026"));
            assertTrue(testo.contains("Scadenza 30 Novembre 2026"));
            assertTrue(testo.contains("CALYPSO S.R.L."));
            assertTrue(testo.contains("Pagina 1 di 5"));
            assertTrue(testo.contains("Le fatture scadute alla data del 15/11/2026 risultano pagate"));
            // pagina 2
            assertTrue(testo.contains("IT001E04318295"));
            assertTrue(testo.contains("23,584 kWh"));
            assertTrue(testo.contains("10/2026: F1 0,238375 €/kWh"));
            // pagina 3
            assertTrue(testo.contains("Perdite Quota Energia F1"));
            assertTrue(testo.contains("€ 16,97"));
            assertTrue(testo.contains("Corrispettivo Morosità Energia"));
            // la rettifica a 0 non compare
            assertTrue(!testo.contains("Rettifica oneri di programmazione"));
        }
    }

    @Test
    void rettificaPresenteSeDiversaDaZero() throws IOException {
        Fattura fattura = fatturaDiEsempio(1);
        FatturaSingola fs = fattura.getFattureSingole().get(0);
        fs.getFornitura().setRettificaProgrammazione(-3.5);
        new CalcoloFatturaService().calcola(fs, CalcoloFatturaServiceTest.punDiEsempio());

        try (PDDocument documento = Loader.loadPDF(new PdfFatturaService().pdf(fattura))) {
            assertEquals(3, documento.getNumberOfPages());
            String testo = new PDFTextStripper().getText(documento);
            assertTrue(testo.contains("Rettifica oneri di programmazione"));
            assertTrue(testo.contains("€ -3,50"));
        }
    }

    private static Fattura fatturaDiEsempio(int forniture) {
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setRagioneSociale("CALYPSO S.R.L.");
        cliente.setIndirizzo("Via Cesare Battisti 31");
        cliente.setCap(65122);
        cliente.setComune("Pescara");
        cliente.setProvincia("PE");
        cliente.setPIva("01234567890");

        Fattura fattura = new Fattura();
        fattura.setNumeroFattura("AUEE202611007");
        fattura.setMese(10);
        fattura.setAnno(2026);
        fattura.setDataFattura(LocalDate.of(2026, 11, 15));
        fattura.setCliente(cliente);

        for (int i = 0; i < forniture; i++) {
            FatturaSingola fs = CalcoloFatturaServiceTest.fatturaDiEsempio();
            fs.setFattura(fattura);
            Fornitura fornitura = fs.getFornitura();
            fornitura.setCliente(cliente);
            fornitura.setIndirizzo("Via Roma 1");
            fornitura.setCap(100);
            fornitura.setComune("Roma");
            fornitura.setProvincia("RM");
            fornitura.setPotenzaDisponibile(30.0);
            fornitura.setTipoContatore(org.emmek.IEG.enums.TipoContatore.ORARIO);
            fornitura.getOfferta().setNome("Energia PUN");
            fornitura.getOfferta().setCodice("PUN01");
            fornitura.getOfferta().setDataInizio(LocalDate.of(2026, 1, 1));
            lettura(fs.getLetturaIniziale(), 1200.5, 900.25, 2100.0);
            lettura(fs.getLetturaFinale(), 1207.715, 906.311, 2110.308);
            fs.getLetturaFinale().setPotF1(0.208);
            // solo la prima fornitura ha un'altra partita
            AltraPartita partita = new AltraPartita();
            partita.setVoce("Corrispettivo Morosità Energia");
            partita.setImporto(332.28);
            new CalcoloFatturaService().calcola(fs, CalcoloFatturaServiceTest.punDiEsempio(), i == 0 ? List.of(partita) : List.of());
            fattura.addFatturaSingola(fs);
        }
        List<FatturaSingola> singole = fattura.getFattureSingole();
        fattura.setConsumoTot(singole.stream().mapToDouble(FatturaSingola::getConsumoTot).sum());
        fattura.setTotaleMateria(singole.stream().mapToDouble(FatturaSingola::getTotaleMateria).sum());
        fattura.setTotaleTrasporto(singole.stream().mapToDouble(FatturaSingola::getTotaleTrasporto).sum());
        fattura.setTotaleOneri(singole.stream().mapToDouble(FatturaSingola::getTotaleOneri).sum());
        fattura.setTotaleImposte(singole.stream().mapToDouble(FatturaSingola::getTotaleImposte).sum());
        fattura.setTotaleImponibile(singole.stream().mapToDouble(FatturaSingola::getTotaleImponibile).sum());
        fattura.setTotaleIva(singole.stream().mapToDouble(FatturaSingola::getTotaleIva).sum());
        return fattura;
    }

    private static void lettura(Lettura lettura, double f1, double f2, double f3) {
        lettura.setTipoLettura(TipoLettura.REALE);
        lettura.setEaF1(f1);
        lettura.setEaF2(f2);
        lettura.setEaF3(f3);
        lettura.setKa(1.0);
        lettura.setKp(1.0);
    }
}
