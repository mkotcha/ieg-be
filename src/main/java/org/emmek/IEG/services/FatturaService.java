package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.repositories.AltraPartitaRepository;
import org.emmek.IEG.repositories.FatturaRepository;
import org.emmek.IEG.repositories.FatturaSingolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FatturaService {

    @Autowired
    private FatturaRepository fatturaRepository;

    @Autowired
    private LetturaService letturaService;

    @Autowired
    private OneriService oneriService;

    @Autowired
    private DispacciamentoService dispacciamentoService;

    @Autowired
    private FatturaSingolaRepository fatturaSingolaRepository;

    @Autowired
    private PdfFatturaService pdfFatturaService;

    @Autowired
    private PunService punService;

    @Autowired
    private CalcoloFatturaService calcoloFatturaService;

    @Autowired
    private AltraPartitaRepository altraPartitaRepository;


    /**
     * Calcola e salva la fattura del mese di consumo e ne genera il pdf.
     */
    public Fattura newfattura(Cliente cliente, Integer mese, Integer anno) throws IOException {
        Fattura fattura = calcolaFattura(cliente, mese, anno);
        pdfFatturaService.createFattura(fattura);
        return fattura;
    }

    /**
     * Calcola e salva la fattura del mese di consumo (fatture singole, voci e totali), senza generare i file.
     */
    public Fattura calcolaFattura(Cliente cliente, Integer mese, Integer anno) {
        // numero come nelle fatture di esempio: AUEE + anno/mese di emissione (mese successivo al consumo) + id cliente
        LocalDate meseEmissione = LocalDate.of(anno, mese, 1).plusMonths(1);
        String numeroFattura = "AUEE" + meseEmissione.getYear() +
                String.format("%02d", meseEmissione.getMonthValue()) +
                String.format("%03d", cliente.getId());
        Fattura fattura;
        if (fatturaRepository.existsByNumeroFattura(numeroFattura)) {
            log.debug("Fattura " + numeroFattura + " già presente... Sovrascrivo");
            fattura = fatturaRepository.findByNumeroFattura(numeroFattura).orElseThrow(() -> new RuntimeException("Fattura " + numeroFattura + " non trovata"));
            List<FatturaSingola> fatturaSingole = fattura.getFattureSingole();
            fatturaSingole.forEach(fatturaSingola -> {
                fatturaSingolaRepository.delete(fatturaSingola);
            });
            fattura.setFattureSingole(null);
        } else {
            fattura = new Fattura();
        }
        List<Fornitura> forniture = cliente.getForniture();
        fattura.setMese(mese);
        fattura.setAnno(anno);
        fattura.setDataFattura(LocalDate.now());
        fattura.setCliente(cliente);
        fattura.setNumeroFattura(numeroFattura);
        fatturaRepository.save(fattura);
        forniture.forEach(fornitura -> {
            FatturaSingola fatturaSingola = new FatturaSingola();
            fatturaSingola.setFattura(fattura);
            fatturaSingola.setFornitura(fornitura);
            fatturaSingola.setOneri(oneriService.getOneri(fornitura, mese, anno));
            // il dispacciamento è mensile (gli oneri invece sono trimestrali)
            fatturaSingola.setDispacciamento(dispacciamentoService.findByMeseAndAnno(mese, anno));
            fatturaSingola.setLetture(letturaService.getLetture(fornitura, mese, anno));
            // getLetture restituisce le letture ordinate per data decrescente
            fatturaSingola.setLetturaFinale(fatturaSingola.getLetture().get(0));
            fatturaSingola.setLetturaIniziale(fatturaSingola.getLetture().get(fatturaSingola.getLetture().size() - 1));
            Map<String, Double> consumi = letturaService.getConsumi(fatturaSingola.getLetture());
            fatturaSingola.setConsumoF1(consumi.get("EaF1"));
            fatturaSingola.setConsumoF2(consumi.get("EaF2"));
            fatturaSingola.setConsumoF3(consumi.get("EaF3"));
            fatturaSingola.setPerditeF1(consumi.get("perditeF1"));
            fatturaSingola.setPerditeF2(consumi.get("perditeF2"));
            fatturaSingola.setPerditeF3(consumi.get("perditeF3"));
            fatturaSingola.setConsumoF1r(consumi.get("ErF1"));
            fatturaSingola.setConsumoF2r(consumi.get("ErF2"));
            fatturaSingola.setConsumoF3r(consumi.get("ErF3"));
            fatturaSingola.setConsumoTot(consumi.get("consumoTot"));
            fatturaSingola.setConsumoTotP(consumi.get("consumoTotP"));
            fatturaSingola.setConsumoTotR(consumi.get("consumoTotR"));
            fatturaSingola.setConsumoF1rc(consumi.get("ErcF1"));
            fatturaSingola.setConsumoF2rc(consumi.get("ErcF2"));
            fatturaSingola.setConsumoF3rc(consumi.get("ErcF3"));
            fatturaSingola.setConsumoTotRc(consumi.get("ErcF1") + consumi.get("ErcF2") + consumi.get("ErcF3"));
            fatturaSingola.setConsumoTotVerifica(letturaService.getConsumoTotPrimaUltima(
                    fatturaSingola.getLetturaIniziale(), fatturaSingola.getLetturaFinale()));
            if (Math.abs(fatturaSingola.getConsumoTotVerifica() - fatturaSingola.getConsumoTot()) > 0.001)
                log.warn("Fornitura {}: consumoTot {} diverso da consumo prima/ultima lettura {}", fornitura.getId(),
                        fatturaSingola.getConsumoTot(), fatturaSingola.getConsumoTotVerifica());
            fatturaSingola.setPotenzaPrelevata(consumi.get("potMax"));
            // tutte le voci del dettaglio, i totali e i valori dello scontrino
            // PUN specifico del POD se presente, altrimenti quello generico; non serve per le offerte a prezzo fisso
            Offerta offerta = fornitura.getOfferta();
            Pun pun = offerta != null && offerta.isPun() ? punService.getPun(fornitura, mese, anno) : null;
            calcoloFatturaService.calcola(fatturaSingola, pun,
                    altraPartitaRepository.findByFornituraAndAttivaTrueAndMeseAndAnnoOrderById(fornitura, mese, anno));
            fatturaSingolaRepository.save(fatturaSingola);
            fattura.addFatturaSingola(fatturaSingola);
        });
        fattura.setConsumoTot(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getConsumoTot).sum());
        fattura.setConsumoTotP(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getConsumoTotP).sum());
        fattura.setTotaleMateria(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleMateria).sum());
        fattura.setTotaleTrasporto(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleTrasporto).sum());
        fattura.setTotaleOneri(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleOneri).sum());
        fattura.setTotaleImposte(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleImposte).sum());
        fattura.setTotaleImponibile(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleImponibile).sum());
        fattura.setTotaleIva(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getTotaleIva).sum());
        fattura.setConsumoTotR(fattura.getFattureSingole().stream().mapToDouble(FatturaSingola::getConsumoTotR).sum());
        // salva i totali prima di generare il pdf, così restano anche se la generazione del file fallisce
        fatturaRepository.save(fattura);
        return fattura;
    }

    public Page<Fattura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return fatturaRepository.findAll(pageable);
    }

    public List<Fattura> findByCliente(Cliente cliente) {
        return fatturaRepository.findByCliente(cliente);
    }

    public Fattura findByNumeroFattura(String numeroFattura) {
        return fatturaRepository.findByNumeroFattura(numeroFattura).orElseThrow(() -> new RuntimeException("Fattura " + numeroFattura + " non trovata"));
    }

    public List<Fattura> findAll() {
        return fatturaRepository.findAll();
    }
}
