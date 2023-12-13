package org.emmek.IEG.controllers;

import jakarta.xml.bind.JAXBException;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.services.LetturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
                                    @RequestParam(defaultValue = "id,asc") String sort) {
        String[] parts = sort.split(",");
        String sortField = parts[0];
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (parts.length > 1) {
            sortDirection = parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
        return letturaService.findAll(page, size, Sort.by(sortDirection, sortField));
    }

    @PostMapping("/flussi")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String uploadFlussi(@RequestParam("flussi") MultipartFile body) throws IOException, JAXBException {
        return letturaService.uploadFlussi(body);
    }
}
