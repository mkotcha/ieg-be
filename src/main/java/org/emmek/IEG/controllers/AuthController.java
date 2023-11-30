package org.emmek.IEG.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.payloads.NewUtenteDTO;
import org.emmek.IEG.payloads.UtenteLoginDTO;
import org.emmek.IEG.payloads.UtenteLoginSuccessDTO;
import org.emmek.IEG.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "API gestione utenti")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("login")
    public UtenteLoginSuccessDTO login(@RequestBody UtenteLoginDTO body) {
        return new UtenteLoginSuccessDTO(authService.autheticateUtente(body));
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Utente saveUser(@RequestBody @Validated NewUtenteDTO body, BindingResult validation) {
        if (validation.hasErrors()) {
            throw new BadRequestException(validation.getAllErrors());
        } else {
            try {
                return authService.save(body);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
