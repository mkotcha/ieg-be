package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.services.ClienteService;
import org.emmek.IEG.services.FatturaService;
import org.emmek.IEG.services.FornituraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/clienti")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private FornituraService fornituraService;

    @Autowired
    private FatturaService fatturaService;

    @GetMapping("")
    public Page<Cliente> getClienti(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "id") String sort) {
        return clienteService.findAll(page, size, sort);
    }

    @GetMapping("/{id}/fattura")
    public Fattura setFattura(@PathVariable long id,
                              @RequestParam(required = false) Integer mese,
                              @RequestParam(required = false) Integer anno) throws IOException {
        if (mese == null) {
            if (LocalDate.now().getMonthValue() == 1) {
                mese = 12;
                if (anno == null) anno = LocalDate.now().getYear() - 1;
                else anno = anno - 1;
            } else {
                mese = LocalDate.now().getMonthValue() - 1;
                if (anno == null) anno = LocalDate.now().getYear();
            }
        }
        return clienteService.newFattura(id, mese, anno);
    }

    @GetMapping("{id}")
    public Cliente getCliente(@PathVariable String id) {
        return clienteService.findById(id);
    }

    @GetMapping("/{id}/forniture")
    public List<Fornitura> getForniture(@PathVariable String id) {
        return fornituraService.findByCliente(clienteService.findById(id));
    }

    @GetMapping("/{id}/fatture")
    public List<Fattura> getFatture(@PathVariable String id) {
        return fatturaService.findByCliente(clienteService.findById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        clienteService.delete(id);
    }

}

