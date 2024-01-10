package org.emmek.IEG.services;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FatturaService fatturaService;

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

    public Cliente findById(String idCliente) {
        return clienteRepository.findById(Long.parseLong(idCliente)).orElseThrow(() -> new RuntimeException("Cliente con id: " + idCliente + " non trovato"));
    }

    public Fattura newFattura(long id, Integer mese, Integer anno) throws IOException {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente con id: " + id + " non trovato"));
        return fatturaService.newfattura(cliente, mese, anno);
    }

    public void delete(String id) {
        Cliente cliente = clienteRepository.findById(Long.parseLong(id)).orElseThrow(() -> new RuntimeException("Cliente con id: " + id + " non trovato"));
        clienteRepository.delete(cliente);
    }
}
