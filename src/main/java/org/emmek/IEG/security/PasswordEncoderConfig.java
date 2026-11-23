package org.emmek.IEG.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Separato da SecurityConfig: UtenteService lo usa e JWTAuthFilter usa UtenteService (altrimenti ciclo). */
@Configuration
public class PasswordEncoderConfig {
    @Bean
    PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder(11);
    }
}
