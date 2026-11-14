package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.FatturaFornitore;
import org.emmek.IEG.entities.MappaturaVoceFornitore;
import org.emmek.IEG.entities.VoceFatturaFornitore;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.EsitoImportFatturaFornitoreDTO;
import org.emmek.IEG.payloads.MappaturaVoceFornitoreDTO;
import org.emmek.IEG.payloads.TariffaNonMappataDTO;
import org.emmek.IEG.services.FatturaFornitoreService;
import org.emmek.IEG.services.MappaturaVoceFornitoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Fatture del fornitore all'ingrosso (csv di riepilogo) e mappature delle sue voci sulle voci di confronto.
 * Il confronto con le nostre fatture è in FatturaController (/api/fatture/confronto, /api/fatture/{numero}/confronto).
 */
@RestController
@RequestMapping("/api/fatture-fornitore")
public class FatturaFornitoreController {

    @Autowired
    private FatturaFornitoreService fatturaFornitoreService;

    @Autowired
    private MappaturaVoceFornitoreService mappaturaService;

    /**
     * Importa un csv di riepilogo o uno zip di csv; una bolletta già importata viene sostituita.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<EsitoImportFatturaFornitoreDTO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return fatturaFornitoreService.upload(file);
    }

    @GetMapping("")
    public List<FatturaFornitore> getFatture() {
        return fatturaFornitoreService.findAll();
    }

    /**
     * Righe delle fatture del fornitore, filtrabili per bolletta, pod e mese/anno di competenza.
     */
    @GetMapping("/voci")
    public List<VoceFatturaFornitore> getVoci(@RequestParam(required = false) String numero,
                                              @RequestParam(required = false) String pod,
                                              @RequestParam(required = false) Integer mese,
                                              @RequestParam(required = false) Integer anno) {
        if ((numero == null || numero.isBlank()) && (pod == null || pod.isBlank()) && (mese == null || anno == null)) {
            throw new BadRequestException("Indicare numero, pod o mese e anno");
        }
        return fatturaFornitoreService.findVoci(numero, pod, mese, anno);
    }

    @DeleteMapping("/{numero}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String numero) {
        fatturaFornitoreService.delete(numero);
    }

    @GetMapping("/mappature")
    public List<MappaturaVoceFornitore> getMappature() {
        return mappaturaService.findAll();
    }

    /**
     * Tariffe delle fatture importate che nessuna mappatura riconosce (finiscono in NON_MAPPATA).
     */
    @GetMapping("/mappature/non-mappate")
    public List<TariffaNonMappataDTO> getNonMappate() {
        return mappaturaService.findNonMappate();
    }

    @PostMapping("/mappature")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ADMIN')")
    public MappaturaVoceFornitore saveMappatura(@RequestBody @Validated MappaturaVoceFornitoreDTO body,
                                                BindingResult validation) {
        if (validation.hasErrors()) throw new BadRequestException(validation.getAllErrors());
        return mappaturaService.save(body);
    }

    @PutMapping("/mappature/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public MappaturaVoceFornitore updateMappatura(@PathVariable Long id,
                                                  @RequestBody @Validated MappaturaVoceFornitoreDTO body,
                                                  BindingResult validation) {
        if (validation.hasErrors()) throw new BadRequestException(validation.getAllErrors());
        return mappaturaService.update(id, body);
    }

    @DeleteMapping("/mappature/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteMappatura(@PathVariable Long id) {
        mappaturaService.delete(id);
    }
}
