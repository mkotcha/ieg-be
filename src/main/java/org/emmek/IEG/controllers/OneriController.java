package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Oneri;
import org.emmek.IEG.payloads.OneriDTO;
import org.emmek.IEG.services.OneriService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oneri")
public class OneriController {

    @Autowired
    private OneriService oneriService;

    @GetMapping("")
    public List<Oneri> getOneri() {
        return oneriService.findAll();
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public Oneri save(@RequestBody @Validated OneriDTO body, BindingResult validation) {
        return oneriService.save(body);
    }

    @GetMapping("/{id}")
    public Oneri getOneri(@PathVariable long id) {
        return oneriService.findById(id);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable long id) {
        oneriService.delete(id);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Oneri update(@PathVariable long id, @RequestBody @Validated OneriDTO body, BindingResult validation) {
        return oneriService.update(id, body);
    }


}
