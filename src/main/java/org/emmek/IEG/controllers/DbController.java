package org.emmek.IEG.controllers;

import org.emmek.IEG.services.DumpDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/db")
public class DbController {
    @Autowired
    private DumpDbService dumpDbService;

    /**
     * Dump del database (pg_dump -Fc, da ripristinare con pg_restore); il nome del file è in X-Filename.
     */
    @GetMapping("/dump")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StreamingResponseBody> dump() throws IOException, InterruptedException {
        Path dump = dumpDbService.dump();
        String nomeFile = "ieg-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm")) + ".dump";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeFile).build());
        headers.setContentLength(Files.size(dump));
        headers.add("Access-Control-Expose-Headers", "X-Filename");
        headers.add("X-Filename", nomeFile);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out -> {
                    try {
                        Files.copy(dump, out);
                    } finally {
                        Files.deleteIfExists(dump);
                    }
                });
    }
}
