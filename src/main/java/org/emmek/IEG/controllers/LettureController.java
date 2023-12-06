package org.emmek.IEG.controllers;

import jakarta.xml.bind.JAXBException;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.services.LetturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/letture")
public class LettureController {

    @Autowired
    private LetturaService letturaService;

    @GetMapping("")
    public Page<Lettura> getLetture(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(defaultValue = "id") String sort) {
        return letturaService.findAll(page, size, sort);
    }

    @PostMapping("/flussi")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String uploadFlussi(@RequestParam("flussi") MultipartFile body) throws IOException, JAXBException {
        return letturaService.uploadFlussi(body);
    }
}
