package org.emmek.IEG.controllers;

import jakarta.xml.bind.JAXBException;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.LetturaDTO;
import org.emmek.IEG.services.FornituraService;
import org.emmek.IEG.services.LetturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/letture")
public class LettureController {

    @Autowired
    private LetturaService letturaService;

    @Autowired
    private FornituraService fornituraService;

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
    public void delete(@PathVariable long id) {
        letturaService.delete(id);
    }

    @GetMapping("/{id}")
    public Lettura get(@PathVariable long id) {
        return letturaService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Lettura update(@PathVariable long id, @RequestBody @Validated LetturaDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        } else {
            return letturaService.update(id, body);
        }
    }

    @GetMapping("/conta/{fornituraId}")
    public int contaLetture(@PathVariable String fornituraId,
                            @RequestParam(defaultValue = "0") int mese,
                            @RequestParam(defaultValue = "0") int anno) {
        if (mese == 0 || anno == 0) {
            if (LocalDate.now().getMonthValue() == 1) {
                mese = 12;
                if (anno == 0) anno = LocalDate.now().getYear() - 1;
                else anno = anno - 1;
            } else {
                mese = LocalDate.now().getMonthValue() - 1;
                anno = LocalDate.now().getYear();
            }
        }
        Fornitura fornitura = fornituraService.findById(fornituraId);
        return letturaService.contaLetture(fornitura, mese, anno);
    }
}
