package org.emmek.IEG.controllers;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.services.FatturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@Slf4j
@RequestMapping("/api/fatture")
public class FatturaController {
    @Autowired
    private FatturaService fatturaService;


    @GetMapping("")
    public Iterable<Fattura> getFatture() {
        return fatturaService.findAll();
    }

    @GetMapping(value = "/{numeroFattura}/pdf")
    public ResponseEntity<InputStreamResource> getFatturaPdf(@PathVariable String numeroFattura) throws IOException {
        Fattura fattura = fatturaService.findByNumeroFattura(numeroFattura);

        String src = fattura.getPdf();
        log.debug("PDF to download: " + src);
        InputStream in = new FileInputStream(src);
        InputStreamResource resource = new InputStreamResource(in);

        String originalFilename = fattura.getNumeroFattura() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + originalFilename);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + originalFilename);
        headers.add("Access-Control-Expose-Headers", "X-Filename");
        headers.add("X-Filename", originalFilename); // Add filename in a separate header

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping(value = "/{numeroFattura}/xlsx")
    public ResponseEntity<InputStreamResource> getFatturaXlsx(@PathVariable String numeroFattura) throws IOException {
        Fattura fattura = fatturaService.findByNumeroFattura(numeroFattura);

        String src = fattura.getXlsx();
        log.debug("XLSX to download: " + src);
        InputStream in = new FileInputStream(src);
        InputStreamResource resource = new InputStreamResource(in);

        String originalFilename = fattura.getNumeroFattura() + ".xlsx";

        HttpHeaders headers = new HttpHeaders();

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + originalFilename);
        headers.add("Access-Control-Expose-Headers", "X-Filename");
        headers.add("X-Filename", originalFilename); // Add filename in a separate header

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
