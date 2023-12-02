package org.emmek.IEG.utils;


import com.poiji.bind.Poiji;
import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Indirizzo;
import org.emmek.IEG.entities.Ruolo;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.helpers.ClienteModel;
import org.emmek.IEG.services.ClienteService;
import org.emmek.IEG.services.IndirizzoService;
import org.emmek.IEG.services.RuoloService;
import org.emmek.IEG.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class Runner implements CommandLineRunner {

    @Autowired
    RuoloService ruoloService;

    @Autowired
    UtenteService utenteService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    IndirizzoService indirizzoService;


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
        importClienti();
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

    public void importClienti() {
        List<ClienteModel> clienti = Poiji.fromExcel(new File("data/clienti.xlsx"), ClienteModel.class);
        for (ClienteModel clienteModel : clienti) {
            Cliente cliente = new Cliente();
            cliente.setId(clienteModel.getId());
            cliente.setRagioneSociale(clienteModel.getRagioneSociale());
            cliente.setPIva(clienteModel.getPIva());
            cliente.setCf(clienteModel.getCf());
            cliente.setTelefono(clienteModel.getTelefono());
            cliente.setEmail(clienteModel.getEmail());
            Indirizzo indirizzo = new Indirizzo();
            indirizzo.setIndirizzo(clienteModel.getIndirizzo());
            indirizzo.setCap(clienteModel.getCap());
            indirizzo.setProvincia(clienteModel.getProvincia());
            indirizzo.setComune(clienteModel.getComune());
            indirizzoService.save(indirizzo);
            cliente.setIndirizzo(indirizzo);
            clienteService.save(cliente);
        }
    }
}
