package org.emmek.IEG.services;

import org.emmek.IEG.entities.Lettura;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Consumi dalle letture, senza Spring né database.
 */
class LetturaServiceTest {

    private final LetturaService service = new LetturaService();

    private static Lettura lettura(int giorno, double ea, double potenza, boolean manuale) {
        Lettura l = new Lettura();
        l.setDataLettura(LocalDate.of(2026, 10, giorno));
        l.setEaF1(ea);
        l.setEaF2(0.0);
        l.setEaF3(0.0);
        l.setErF1(0.0);
        l.setErF2(0.0);
        l.setErF3(0.0);
        l.setPotF1(potenza);
        l.setPotF2(0.0);
        l.setPotF3(0.0);
        l.setKa(1.0);
        l.setKr(1.0);
        l.setKp(1.0);
        l.setManuale(manuale);
        return l;
    }

    @Test
    void senzaLettureManualiIlPiccoEIlMassimoDelPeriodo() {
        Map<String, Double> consumi = service.getConsumi(List.of(
                lettura(1, 100, 5, false), lettura(15, 160, 30, false), lettura(31, 220, 20, false)));
        assertEquals(120, consumi.get("consumoTot"), 1e-9);
        assertEquals(30, consumi.get("potMax"), 1e-9);
    }

    @Test
    void conUnaLetturaManualeContanoSoloPrimaEUltima() {
        // la lettura intermedia del fornitore (picco 30) viene ignorata
        Map<String, Double> consumi = service.getConsumi(List.of(
                lettura(1, 100, 5, false), lettura(15, 160, 30, false), lettura(31, 250, 20, true)));
        assertEquals(150, consumi.get("consumoTot"), 1e-9);
        assertEquals(20, consumi.get("potMax"), 1e-9);
    }

    @Test
    void conUnaLetturaManualeLaListaDellaFatturaSiRiduceAPrimaEUltima() {
        Lettura prima = lettura(1, 100, 5, false);
        Lettura ultima = lettura(31, 250, 20, false);
        List<Lettura> letture = LetturaService.primaEUltimaSeManuali(List.of(ultima, lettura(15, 160, 30, true), prima));
        assertEquals(List.of(ultima, prima), letture);
    }
}
