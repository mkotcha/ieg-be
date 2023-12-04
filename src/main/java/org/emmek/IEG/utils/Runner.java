package org.emmek.IEG.utils;


import com.poiji.bind.Poiji;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Ruolo;
import org.emmek.IEG.entities.Utente;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.helpers.ClienteModel;
import org.emmek.IEG.helpers.FlussoMisure;
import org.emmek.IEG.services.ClienteService;
import org.emmek.IEG.services.RuoloService;
import org.emmek.IEG.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

@Component
public class Runner implements CommandLineRunner {

    @Autowired
    RuoloService ruoloService;

    @Autowired
    UtenteService utenteService;

    @Autowired
    ClienteService clienteService;

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
//        importClienti();
        FlussoMisure flussoMisure = unmarshal();
        System.out.println(flussoMisure.toString());
    }

    public FlussoMisure unmarshal() throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(FlussoMisure.class);
        return (FlussoMisure) context.createUnmarshaller()
                .unmarshal(new FileReader("data/test_o.xml"));
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
            cliente.setIndirizzo(clienteModel.getIndirizzo());
            cliente.setCap(clienteModel.getCap());
            cliente.setProvincia(clienteModel.getProvincia());
            cliente.setComune(clienteModel.getComune());
            clienteService.save(cliente);
        }
    }
}
