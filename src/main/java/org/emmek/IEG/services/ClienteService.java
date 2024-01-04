package org.emmek.IEG.services;

import org.emmek.IEG.entities.*;
import org.emmek.IEG.repositories.ClienteRepository;
import org.emmek.IEG.repositories.LetturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private LetturaRepository letturaRepository;

    public Page<Cliente> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return clienteRepository.findAll(pageable);
    }

    public void save(Cliente cliente) {
        clienteRepository.save(cliente);
    }

    public Cliente findById(long idCliente) {
        return clienteRepository.findById(idCliente).orElseThrow(() -> new RuntimeException("Cliente con id: " + idCliente + " non trovato"));
    }

    public Fattura setFattura(long id, Integer mese, Integer anno) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente con id: " + id + " non trovato"));
        List<Fornitura> forniture = cliente.getForniture();
        Fattura fattura = new Fattura();
        fattura.setMese(mese);
        fattura.setAnno(anno);

        fattura.setCliente(cliente);
        fattura.setNumeroFattura("AUEE2023" +
                String.format("%20d", mese) +
                String.format("%03d", cliente.getId()) + "000");
        forniture.forEach(fornitura -> {
            FatturaSingola fatturaSingola = new FatturaSingola();
            fatturaSingola.setFattura(fattura);
            fatturaSingola.setFornitura(fornitura);
            LocalDate from;
            LocalDate to;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (mese == 1) {
                from = LocalDate.parse((anno - 1) + "-12-15", formatter);
            } else {
                from = LocalDate.parse(anno + "-" + String.format("%02d", mese - 1) + "-15", formatter);
            }
            if (mese == 12) {
                to = LocalDate.parse((anno + 1) + "-01-15", formatter);
            } else {
                to = LocalDate.parse(anno + "-" + String.format("%02d", mese + 1) + "-15", formatter);
            }
            List<Lettura> letture = letturaRepository.findByFornituraIdAndDataLetturaBetween(fornitura.getId(), from, to);
            fatturaSingola.setLetture(letture);
            fattura.addFatturaSingola(fatturaSingola);
        });
        return fattura;
    }
}
