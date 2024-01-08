package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.entities.FatturaSingola;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.repositories.FatturaRepository;
import org.emmek.IEG.repositories.FatturaSingolaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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


    public Fattura newfattura(Cliente cliente, Integer mese, Integer anno) {
        String numeroFattura = "AUEE" + anno +
                String.format("%02d", mese) +
                String.format("%03d", cliente.getId()) + "000";
        Fattura fattura;
        if (fatturaRepository.existsByNumeroFattura(numeroFattura)) {
            log.debug("Fattura " + numeroFattura + " già presente... Sovrascrivo");
            fattura = fatturaRepository.findByNumeroFattura(numeroFattura);
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
            fatturaSingola.setConsumoF1r(consumi.get("ErF1"));
            fatturaSingola.setConsumoF2r(consumi.get("ErF2"));
            fatturaSingola.setConsumoF3r(consumi.get("ErF3"));
            fatturaSingolaRepository.save(fatturaSingola);
            fattura.addFatturaSingola(fatturaSingola);
        });
        return fattura;
    }

    public Page<Fattura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return fatturaRepository.findAll(pageable);
    }

    public List<Fattura> findByCliente(Cliente cliente) {
        return fatturaRepository.findByCliente(cliente);
    }
}
