package org.emmek.IEG.services;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.entities.FatturaSingola;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FatturaService {

    @Autowired
    private FatturaRepository fatturaRepository;

    @Autowired
    private LetturaService letturaService;

    @Autowired
    private OneriService oneriService;

    @Autowired
    private DispacciamentoService dispacciamentoService;


    public Fattura newfattura(Cliente cliente, Integer mese, Integer anno) {
        int trimestre = (mese - 1) / 3 + 1;
        List<Fornitura> forniture = cliente.getForniture();
        Fattura fattura = new Fattura();
        fattura.setMese(mese);
        fattura.setAnno(anno);
        fattura.setCliente(cliente);
        fattura.setNumeroFattura("AUEE" + anno +
                String.format("%02d", mese) +
                String.format("%03d", cliente.getId()) + "000");
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

            fattura.addFatturaSingola(fatturaSingola);
        });
        return fattura;
    }

    public Page<Fattura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return fatturaRepository.findAll(pageable);
    }
}
