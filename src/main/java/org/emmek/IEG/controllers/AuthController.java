package org.emmek.IEG.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.emmek.IEG.payloads.UtenteLoginDTO;
import org.emmek.IEG.payloads.UtenteLoginSuccessDTO;
import org.emmek.IEG.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** Login. Non c'è registrazione pubblica: gli utenti li crea un ADMIN con {@code POST /api/utenti}. */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Login")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public UtenteLoginSuccessDTO login(@RequestBody UtenteLoginDTO body) {
        return authService.autheticateUtente(body);
    }
}
