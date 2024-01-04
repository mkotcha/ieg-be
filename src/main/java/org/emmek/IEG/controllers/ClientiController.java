package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.services.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/clienti")
public class ClientiController {
    @Autowired
    private ClienteService clienteService;

    @GetMapping("")
    public Page<Cliente> getClienti(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "id") String sort) {
        return clienteService.findAll(page, size, sort);
    }

    @GetMapping("/{id}/fattura")
    public Fattura setFattura(@PathVariable long id,
                              @RequestParam(required = false) Integer mese,
                              @RequestParam(required = false) Integer anno) {
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
        return clienteService.setFattura(id, mese, anno);
    }
}

