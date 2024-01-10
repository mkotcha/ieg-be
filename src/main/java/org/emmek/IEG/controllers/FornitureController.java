package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.services.FornituraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forniture")
public class FornitureController {
    @Autowired
    private FornituraService fornituraService;

    @GetMapping("")
    public Page<Fornitura> getClienti(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(defaultValue = "id") String sort) {
        return fornituraService.findAll(page, size, sort);
    }

    @GetMapping("/listapod")
    public List<String> getListaPod() {
        return fornituraService.getListaPod();
    }

    @GetMapping("{id}")
    public Fornitura getFornitura(@PathVariable String id) {
        return fornituraService.findById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        fornituraService.delete(id);
    }
}
