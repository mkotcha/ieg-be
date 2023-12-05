package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.services.FornituraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
