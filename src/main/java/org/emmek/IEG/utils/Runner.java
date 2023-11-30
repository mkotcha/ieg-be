package org.emmek.IEG.utils;


import org.emmek.IEG.entities.Ruolo;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.services.RuoloService;
import org.emmek.IEG.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Runner implements CommandLineRunner {

    @Autowired
    RuoloService ruoloService;

    @Autowired
    UtenteService utenteService;


    @Value("${admin.username}")
    private String username;

    @Value("${admin.password}")
    private String password;

    @Value("${admin.email}")
    private String email;

    @Autowired
    private PasswordEncoder bcrypt;

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotExist("ADMIN");
        createRoleIfNotExist("USER");
        crateAdminIfNotExist(username);
    }

    private void crateAdminIfNotExist(String username) {
        try {
            utenteService.findByUsername(username);
        } catch (NotFoundException e) {
            Utente user = new Utente();
            user.setUsername(username);
            user.setPassword(bcrypt.encode(password));
            user.setEmail(email);
            user.addRuolo(ruoloService.findByRuolo("ADMIN"));
            utenteService.save(user);
        }
    }

    private void createRoleIfNotExist(String roleName) {
        try {
            ruoloService.findByRuolo(roleName);
        } catch (NotFoundException e) {
            Ruolo ruolo = new Ruolo();
            ruolo.setRuolo(roleName);
            ruoloService.save(ruolo);
        }
    }
}
