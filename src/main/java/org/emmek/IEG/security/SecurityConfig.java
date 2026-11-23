package org.emmek.IEG.security;

import org.emmek.IEG.exceptions.ExceptionsHandlerFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    ExceptionsHandlerFilter exceptionsHandlerFilter;
    @Autowired
    private JWTAuthFilter jwtAuthFilter;
    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;
    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.cors(withDefaults());

        // Regola di base: chi ha fatto login (anche USER, sola lettura) può leggere tutto ciò che è GET sotto /api;
        // ogni altra chiamata sotto /api richiede ADMIN, così un endpoint nuovo senza @PreAuthorize resta chiuso.
        // Eccezioni: le GET con effetti o dati sensibili e la gestione utenti sono solo ADMIN; il proprio profilo
        // (/api/utenti/me, compreso il cambio password) è di tutti.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/clienti/*/fattura").hasAuthority("ADMIN")
                .requestMatchers("/api/db/**").hasAuthority("ADMIN")
                .requestMatchers("/api/utenti/me", "/api/utenti/me/**").authenticated()
                .requestMatchers("/api/utenti", "/api/utenti/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasAuthority("ADMIN")
                .anyRequest().permitAll());
        // senza questo un 403 delle regole sopra arriverebbe senza corpo: lo passo all'ExceptionsHandler (JSON)
        http.exceptionHandling(ex -> ex.accessDeniedHandler(
                (request, response, e) -> resolver.resolveException(request, response, null, e)));

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(exceptionsHandlerFilter, JWTAuthFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
