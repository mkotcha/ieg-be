package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.LetturaDTO;
import org.emmek.IEG.services.FornituraService;
import org.emmek.IEG.services.LetturaService;
import org.emmek.IEG.enums.TipoLavoro;
import org.emmek.IEG.model.Lavoro;
import org.emmek.IEG.services.LavoroService;
import org.emmek.IEG.services.VerificaLettureService;
import org.emmek.IEG.payloads.VerificaLettureDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/letture")
public class LettureController {

    @Autowired
    private LetturaService letturaService;

    @Autowired
    private FornituraService fornituraService;

    @Autowired
    private LavoroService lavoroService;

    @Autowired
    private VerificaLettureService verificaLettureService;

    @GetMapping("")
    public Page<Lettura> getLetture(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "id,asc") String sort) {
        String[] parts = sort.split(",");
        String sortField = parts[0];
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (parts.length > 1) {
            sortDirection = parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
        return letturaService.findAll(page, size, Sort.by(sortDirection, sortField));
    }

    /**
     * Salva lo zip dei flussi di misura e ne avvia l'import in background; restituisce subito il lavoro, da seguire
     * con GET /api/lavori/{id} (a lavoro completato il risultato è un EsitoImportLettureDTO).
     */
    @PostMapping("/flussi")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Lavoro uploadFlussi(@RequestParam("flussi") MultipartFile body) throws IOException {
        File zip = letturaService.salvaZip(body);
        try {
            return lavoroService.avvia(TipoLavoro.IMPORT_LETTURE, "Import flussi " + body.getOriginalFilename(), lavoro -> {
                try {
                    return letturaService.importaFlussi(zip, lavoro);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e.getMessage(), e);
                }
            });
        } catch (RuntimeException e) {
            // lavoro non avviato (es. un altro import in corso)
            zip.delete();
            throw e;
        }
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Lettura save(@RequestBody @Validated LetturaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        } else {
            return letturaService.save(body);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable Long id) {
        letturaService.delete(id);
    }

    @GetMapping("/{id}")
    public Lettura get(@PathVariable Long id) {
        return letturaService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Lettura update(@PathVariable Long id, @RequestBody @Validated LetturaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        } else {
            return letturaService.update(id, body);
        }
    }

    /**
     * Attiva o disattiva una lettura: quelle disattivate restano in archivio ma non entrano nei calcoli.
     */
    @PutMapping("/{id}/attiva")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Lettura setAttiva(@PathVariable Long id, @RequestParam boolean attiva) {
        return letturaService.setAttiva(id, attiva);
    }

    /**
     * Letture di tutte le forniture che la generazione massiva fatturerebbe per il mese di consumo, con lo stesso
     * periodo del calcolo della fattura e i problemi trovati (letture mancanti, fattura non calcolabile...).
     */
    @GetMapping("/verifica")
    public List<VerificaLettureDTO> verifica(@RequestParam int mese, @RequestParam int anno) {
        return verificaLettureService.verifica(mese, anno);
    }
}
