package org.emmek.IEG.services;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.repositories.FornituraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FornituraService {

    @Autowired
    private FornituraRepository fornituraRepository;

    public Page<Fornitura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return fornituraRepository.findAll(pageable);
    }

    public void save(Fornitura fornitura) {
        fornituraRepository.save(fornitura);
    }

    public Fornitura finById(String pod) {
        return fornituraRepository.findById(pod).orElseThrow(() -> new RuntimeException("Fornitura con id: " + pod + " non trovata"));
    }

    public List<String> getListaPod() {
        return fornituraRepository.getListaPod();
    }

    public Fornitura findById(String id) {
        return fornituraRepository.findById(id).orElseThrow(() -> new RuntimeException("Fornitura con id: " + id + " non trovata"));
    }

    public List<Fornitura> findByCliente(Cliente cliente) {
        return fornituraRepository.findByCliente(cliente);
    }
}
