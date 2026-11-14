package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Offerta;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.OffertaDTO;
import org.emmek.IEG.services.OffertaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offerte")
public class OffertaController {

    @Autowired
    private OffertaService offertaService;

    @GetMapping("")
    public List<Offerta> getOfferte() {
        return offertaService.findAll();
    }

    @GetMapping("/{id}")
    public Offerta getOfferta(@PathVariable Long id) {
        return offertaService.findById(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    public Offerta save(@RequestBody @Validated OffertaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        }
        return offertaService.save(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Offerta update(@PathVariable Long id, @RequestBody @Validated OffertaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        }
        return offertaService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable Long id) {
        offertaService.delete(id);
    }
}
