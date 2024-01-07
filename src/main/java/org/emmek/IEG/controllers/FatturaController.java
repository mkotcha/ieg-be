package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.services.FatturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fatture")
public class FatturaController {
    @Autowired
    private FatturaService fatturaService;


    @GetMapping("")
    public Page<Fattura> getFatture(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "id") String sort) {
        return fatturaService.findAll(page, size, sort);
    }
}
