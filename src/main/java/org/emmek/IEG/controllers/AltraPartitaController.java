package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.AltraPartita;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.AltraPartitaDTO;
import org.emmek.IEG.services.AltraPartitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/altre-partite")
public class AltraPartitaController {

    @Autowired
    private AltraPartitaService altraPartitaService;

    @GetMapping("")
    public List<AltraPartita> getAltrePartite(@RequestParam(required = false) String pod,
                                              @RequestParam(required = false) Integer mese,
                                              @RequestParam(required = false) Integer anno) {
        return altraPartitaService.findAll(pod, mese, anno);
    }

    @GetMapping("/{id}")
    public AltraPartita getAltraPartita(@PathVariable Long id) {
        return altraPartitaService.findById(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    public AltraPartita save(@RequestBody @Validated AltraPartitaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        }
        return altraPartitaService.save(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public AltraPartita update(@PathVariable Long id, @RequestBody @Validated AltraPartitaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        }
        return altraPartitaService.update(id, body);
    }

    /**
     * Attiva o disattiva la partita: quelle disattivate non vanno in fattura (vale per le fatture calcolate dopo).
     */
    @PutMapping("/{id}/attiva")
    @PreAuthorize("hasAuthority('ADMIN')")
    public AltraPartita setAttiva(@PathVariable Long id, @RequestParam boolean attiva) {
        return altraPartitaService.setAttiva(id, attiva);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable Long id) {
        altraPartitaService.delete(id);
    }
}
