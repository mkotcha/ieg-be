package org.emmek.IEG.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.CambioPasswordDTO;
import org.emmek.IEG.payloads.NewUtenteDTO;
import org.emmek.IEG.payloads.UtenteRispostaDTO;
import org.emmek.IEG.payloads.UtenteUpdateDTO;
import org.emmek.IEG.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Utenti. {@code /me} (profilo e cambio della propria password) è di tutti gli utenti; il resto è solo ADMIN.
 */
@RestController
@RequestMapping("/api/utenti")
@Tag(name = "Utenti", description = "Gestione utenti e ruoli")
public class UtenteController {
    @Autowired
    private UtenteService utenteService;

    @GetMapping("/me")
    public UtenteRispostaDTO me(@AuthenticationPrincipal Utente corrente) {
        return UtenteRispostaDTO.of(corrente);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiaPassword(@AuthenticationPrincipal Utente corrente,
                               @RequestBody @Validated CambioPasswordDTO body, BindingResult validation) {
        valida(validation);
        utenteService.cambiaPassword(corrente, body);
    }

    @GetMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UtenteRispostaDTO> elenco() {
        return utenteService.findAll().stream().map(UtenteRispostaDTO::of).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UtenteRispostaDTO get(@PathVariable Long id) {
        return UtenteRispostaDTO.of(utenteService.findById(id));
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UtenteRispostaDTO crea(@RequestBody @Validated NewUtenteDTO body, BindingResult validation) {
        valida(validation);
        return UtenteRispostaDTO.of(utenteService.crea(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UtenteRispostaDTO aggiorna(@PathVariable Long id, @AuthenticationPrincipal Utente corrente,
                                      @RequestBody @Validated UtenteUpdateDTO body, BindingResult validation) {
        valida(validation);
        return UtenteRispostaDTO.of(utenteService.aggiorna(id, body, corrente));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @RequestBody @Validated CambioPasswordDTO body,
                              BindingResult validation) {
        valida(validation);
        utenteService.resetPassword(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long id, @AuthenticationPrincipal Utente corrente) {
        utenteService.elimina(id, corrente);
    }

    private static void valida(BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        }
    }
}
