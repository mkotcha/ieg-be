package org.emmek.IEG.controllers;

import org.emmek.IEG.enums.TipoLavoro;
import org.emmek.IEG.model.Lavoro;
import org.emmek.IEG.services.LavoroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stato delle operazioni lunghe avviate in background (generazione fatture, import letture), per il polling del fe.
 */
@RestController
@RequestMapping("/api/lavori")
public class LavoriController {

    @Autowired
    private LavoroService lavoroService;

    @GetMapping("/{id}")
    public Lavoro getLavoro(@PathVariable String id) {
        return lavoroService.findById(id);
    }

    /**
     * L'ultimo lavoro del tipo (in corso o terminato), per riprendere a seguirlo dopo aver ricaricato la pagina;
     * 204 se non ce ne sono.
     */
    @GetMapping("/ultimo")
    public ResponseEntity<Lavoro> getUltimo(@RequestParam TipoLavoro tipo) {
        return lavoroService.ultimo(tipo).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
}
