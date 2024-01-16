package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Dispacciamento;
import org.emmek.IEG.payloads.DispacciamentoDTO;
import org.emmek.IEG.services.DispacciamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispacciamento")
public class DispacciamentoController {

    @Autowired
    private DispacciamentoService dispacciamentoService;

    @GetMapping("")
    public List<Dispacciamento> getDispacciamento() {
        return dispacciamentoService.findAll();
    }

    @GetMapping("/{id}")
    public Dispacciamento getDispacciamento(@PathVariable long id) {
        return dispacciamentoService.findById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable long id) {
        dispacciamentoService.delete(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    public Dispacciamento save(@RequestBody @Validated DispacciamentoDTO body, BindingResult validation) {
        return dispacciamentoService.save(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Dispacciamento update(@PathVariable long id, @RequestBody @Validated DispacciamentoDTO body, BindingResult validation) {
        return dispacciamentoService.update(id, body);
    }

}
