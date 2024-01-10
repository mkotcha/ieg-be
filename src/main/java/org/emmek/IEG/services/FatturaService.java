package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.*;
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
    private ExcelService excelService;

    @Autowired
    private PunService punService;


    public Fattura newfattura(Cliente cliente, Integer mese, Integer anno) throws IOException {
        String numeroFattura = "AUEE" + anno +
                String.format("%02d", mese) +
                String.format("%03d", cliente.getId()) + "00";
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
        int trimestre = (mese - 1) / 3 + 1;
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
            fatturaSingola.setDispacciamento(dispacciamentoService.findByTrimestreAndAnno(trimestre, anno));
            fatturaSingola.setLetture(letturaService.getLetture(fornitura, mese, anno));
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
            fatturaSingola.setPotenzaPrelevata(consumi.get("potMax"));
            fatturaSingola.setTotaleImposte(consumi.get("consumoTot") * 0.0125);
            double totaleMateria;
            double parzialeMateria;
            Pun pun = punService.getByMeseAndAnno(mese, anno);
            Dispacciamento dispacciamento = fatturaSingola.getDispacciamento();
            parzialeMateria = 0 +
                    fatturaSingola.getConsumoF1() * pun.getF1() +
                    fatturaSingola.getConsumoF2() * pun.getF2() +
                    fatturaSingola.getConsumoF3() * pun.getF3() +
                    fatturaSingola.getPerditeF1() * pun.getF1() +
                    fatturaSingola.getPerditeF2() * pun.getF2() +
                    fatturaSingola.getPerditeF3() * pun.getF3() +
                    fatturaSingola.getConsumoTotP() * fatturaSingola.getFornitura().getPrezzo().getSpread();
            totaleMateria = parzialeMateria +
                    fatturaSingola.getFornitura().getProgrammazione().getCommercializzazione() +
                    dispacciamento.getCostoAm() +
                    (dispacciamento.getMsd() * fatturaSingola.getConsumoTotP()) +
                    (dispacciamento.getSicurezza() * fatturaSingola.getConsumoTotP()) +
                    (dispacciamento.getEolico() * fatturaSingola.getConsumoTotP()) +
                    (dispacciamento.getDis() * fatturaSingola.getConsumoTotP()) +
                    (dispacciamento.getCapacita() * fatturaSingola.getConsumoTotP()) +
                    (fatturaSingola.getFornitura().getProgrammazione().getOneriProgrammazione() * parzialeMateria / 100) +
                    (dispacciamento.getInt73() * fatturaSingola.getConsumoTotP());
            fatturaSingola.setTotaleMateria(totaleMateria);
            Oneri oneri = fatturaSingola.getOneri();
            double totaleTrasporto = 0 +
                    oneri.getQfTud() +
                    oneri.getQfMis() +
                    oneri.getQpTdm() * fatturaSingola.getFornitura().getPotenzaImpegnata() +
                    oneri.getQeTud() * fatturaSingola.getConsumoTot() +
                    oneri.getQeUc3() * fatturaSingola.getConsumoTot() +
                    dispacciamento.getTrasmissione() * fatturaSingola.getConsumoTot();
            fatturaSingola.setTotaleTrasporto(totaleTrasporto);
            double totaleOneri = 0 +
                    oneri.getQfAsos() +
                    oneri.getQfArim() +
                    oneri.getQpAsos() * fatturaSingola.getFornitura().getPotenzaImpegnata() +
                    oneri.getQpArim() * fatturaSingola.getFornitura().getPotenzaImpegnata() +
                    oneri.getQeAsos() * fatturaSingola.getConsumoTot() +
                    oneri.getQeArim() * fatturaSingola.getConsumoTot();
            fatturaSingola.setTotaleOneri(totaleOneri);
            fatturaSingola.setTotaleImponibile(totaleMateria + totaleTrasporto + totaleOneri + fatturaSingola.getTotaleImposte());
            fatturaSingola.setTotaleIva(fatturaSingola.getTotaleImponibile() * fatturaSingola.getFornitura().getIva() / 100);
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
        excelService.createFattura(fattura);
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
}
