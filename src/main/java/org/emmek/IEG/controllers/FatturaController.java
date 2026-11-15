package org.emmek.IEG.controllers;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.enums.TipoLavoro;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.model.Lavoro;
import org.emmek.IEG.services.LavoroService;
import org.emmek.IEG.repositories.FatturaRepository;
import org.emmek.IEG.payloads.FatturaElencoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.emmek.IEG.payloads.ConfrontoFatturaDTO;
import org.emmek.IEG.payloads.ConfrontoMeseDTO;
import org.emmek.IEG.payloads.EsitoFatturaDTO;
import org.emmek.IEG.services.ConfrontoFatturaService;
import org.emmek.IEG.services.FatturaService;
import org.emmek.IEG.services.GenerazioneFattureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/fatture")
public class FatturaController {
    @Autowired
    private FatturaService fatturaService;

    @Autowired
    private GenerazioneFattureService generazioneFattureService;

    @Autowired
    private ConfrontoFatturaService confrontoFatturaService;

    @Autowired
    private LavoroService lavoroService;

    @Autowired
    private FatturaRepository fatturaRepository;

    /**
     * Avvia in background la generazione delle fatture del mese di consumo indicato per tutti i clienti attivi e
     * restituisce subito il lavoro, da seguire con GET /api/lavori/{id}: a lavoro completato il risultato è la
     * lista degli esiti per cliente (EsitoFatturaDTO); un errore su un cliente non blocca gli altri.
     */
    @PostMapping("/genera")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Lavoro generaFatture(@RequestParam int mese, @RequestParam int anno) {
        return lavoroService.avvia(TipoLavoro.GENERAZIONE_FATTURE,
                String.format("Generazione fatture %02d/%d", mese, anno),
                lavoro -> generazioneFattureService.generaFatture(mese, anno, lavoro));
    }


    /**
     * Tutte le fatture in forma ridotta (senza forniture e voci), dalla più recente alla più vecchia.
     */
    @GetMapping("/elenco")
    public List<FatturaElencoDTO> getElenco() {
        return fatturaRepository.findElenco();
    }

    @GetMapping("")
    public Iterable<Fattura> getFatture() {
        return fatturaService.findAll();
    }

    /**
     * Confronto di tutte le fatture emesse per il mese di consumo con le fatture del fornitore (riepilogo per
     * fattura, senza il dettaglio per fornitura).
     */
    @GetMapping("/confronto")
    public ConfrontoMeseDTO confrontoMese(@RequestParam int mese, @RequestParam int anno) {
        return confrontoFatturaService.confrontaMese(mese, anno);
    }

    /**
     * Confronto di dettaglio di una fattura con le righe del fornitore, per fornitura e voce.
     */
    @GetMapping("/{numeroFattura}/confronto")
    public ConfrontoFatturaDTO confronto(@PathVariable String numeroFattura) {
        return confrontoFatturaService.confronta(numeroFattura);
    }

    @GetMapping(value = "/{numeroFattura}/pdf")
    public ResponseEntity<InputStreamResource> getFatturaPdf(@PathVariable String numeroFattura) throws IOException {
        Fattura fattura = fatturaService.findByNumeroFattura(numeroFattura);
        return download(fattura.getPdf(), MediaType.APPLICATION_PDF);
    }

    /**
     * Solo per le fatture generate con Excel: quelle nuove hanno soltanto il pdf (404).
     */
    @GetMapping(value = "/{numeroFattura}/xlsx")
    public ResponseEntity<InputStreamResource> getFatturaXlsx(@PathVariable String numeroFattura) throws IOException {
        Fattura fattura = fatturaService.findByNumeroFattura(numeroFattura);
        return download(fattura.getXlsx(), MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * File della fattura con il nome con cui è salvato, "<numero> - <ragione sociale>" come nelle macro.
     * X-Filename è url-encoded (la ragione sociale può avere spazi e caratteri non ASCII): il fe lo decodifica.
     */
    private ResponseEntity<InputStreamResource> download(String src, MediaType tipo) throws IOException {
        log.debug("File da scaricare: {}", src);
        if (src == null || !Files.isRegularFile(Path.of(src))) throw new NotFoundException("File della fattura non trovato");
        String nomeFile = Path.of(src).getFileName().toString();
        InputStreamResource resource = new InputStreamResource(new FileInputStream(src));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(nomeFile, StandardCharsets.UTF_8).build());
        headers.add("Access-Control-Expose-Headers", "X-Filename");
        headers.add("X-Filename", URLEncoder.encode(nomeFile, StandardCharsets.UTF_8).replace("+", "%20"));

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(tipo)
                .body(resource);
    }
}
