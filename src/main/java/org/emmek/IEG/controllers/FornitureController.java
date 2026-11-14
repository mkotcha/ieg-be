package org.emmek.IEG.controllers;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.payloads.FornituraDTO;
import org.emmek.IEG.services.ClienteService;
import org.emmek.IEG.services.FornituraService;
import org.emmek.IEG.services.LetturaService;
import org.emmek.IEG.payloads.ConsumoMensileDTO;
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

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private LetturaService letturaService;

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

    //post to add a fornitura
    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Fornitura setFornitura(@RequestBody FornituraDTO fornitura) {
        Cliente cliente = clienteService.findById(fornitura.idCliente());
        return fornituraService.setFornitura(fornitura, cliente);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Fornitura updateFornitura(@PathVariable String id, @RequestBody FornituraDTO fornitura) {
        Cliente cliente = clienteService.findById(fornitura.idCliente());
        return fornituraService.updateFornitura(id, fornitura, cliente);
    }

    /**
     * Consumi attivi per fascia degli ultimi mesi (compreso quello in corso), calcolati dalle letture.
     */
    @GetMapping("{id}/consumi")
    public List<ConsumoMensileDTO> getConsumi(@PathVariable String id, @RequestParam(defaultValue = "24") int mesi) {
        return letturaService.consumiMensili(fornituraService.findById(id), Math.max(1, Math.min(mesi, 120)));
    }

    // associa alla fornitura un'offerta esistente, senza toccare gli altri dati
    @PutMapping("{id}/offerta")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Fornitura setOfferta(@PathVariable String id, @RequestParam long offertaId) {
        return fornituraService.setOfferta(id, offertaId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        fornituraService.delete(id);
    }
}
