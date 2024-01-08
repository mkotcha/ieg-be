package org.emmek.IEG.utils;


import com.poiji.bind.Poiji;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.*;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.helpers.excel.ClienteModel;
import org.emmek.IEG.helpers.excel.FornituraModel;
import org.emmek.IEG.helpers.excel.LetturaModel;
import org.emmek.IEG.helpers.xml.FlussoMisure;
import org.emmek.IEG.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class Runner implements CommandLineRunner {

    @Autowired
    RuoloService ruoloService;

    @Autowired
    UtenteService utenteService;

    @Autowired
    ClienteService clienteService;

    @Autowired
    PrezzoService prezzoService;

    @Autowired
    FornituraService fornituraService;

    @Autowired
    LetturaService letturaService;

    @Autowired
    ProgrammazioneService programmazioneService;

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
//        importForniture();
//        importLetture();
    }

    private void importLetture() {
        List<LetturaModel> letture = Poiji.fromExcel(new File("data/letture.xlsx"), LetturaModel.class);
        for (LetturaModel letturaModel : letture) {
            try {
                Fornitura fornitura = fornituraService.finById(letturaModel.getPod());
                Lettura lettura = new Lettura();
                lettura.setFornitura(fornitura);
                switch (letturaModel.getTipoLettura()) {
                    case "Lettura stimata" -> lettura.setTipoLettura(TipoLettura.STIMA);
                    case "Auto lettura" -> lettura.setTipoLettura(TipoLettura.AUTOLETTURA);
                    case "Cambio contatore" -> lettura.setTipoLettura(TipoLettura.CAMBIO);
                    default -> lettura.setTipoLettura(TipoLettura.REALE);
                }
                if (letturaModel.getTipo().equals("NEW")) {
                    lettura.setTipoLettura(TipoLettura.CAMBIO);
                }
                lettura.setId(letturaModel.getId());
                lettura.setDataLettura(LocalDate.parse(letturaModel.getData()));
                lettura.setTipoContatore(TipoContatore.FASCIA);
                lettura.setEaF1(Double.parseDouble(letturaModel.getF1()));
                lettura.setEaF2(Double.parseDouble(letturaModel.getF2()));
                lettura.setEaF3(Double.parseDouble(letturaModel.getF3()));
                lettura.setErF1(Double.parseDouble(letturaModel.getF1r()));
                lettura.setErF2(Double.parseDouble(letturaModel.getF2r()));
                lettura.setErF3(Double.parseDouble(letturaModel.getF3r()));
                lettura.setPotF1(Double.parseDouble(letturaModel.getPF1()));
                lettura.setPotF2(Double.parseDouble(letturaModel.getPF2()));
                lettura.setPotF3(Double.parseDouble(letturaModel.getPF3()));
                lettura.setUtile(true);
                letturaService.save(lettura);
            } catch (Exception e) {
                log.debug("lettura non importata - " + e.getMessage());
            }

        }

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

    private Prezzo createPrezzoIfNotExist(String nome, double maggiorazione, double spread) {
        Prezzo prezzo;
        try {
            prezzo = prezzoService.findByNome(nome);
        } catch (RuntimeException e) {
            prezzo = new Prezzo();
            prezzo.setNome(nome);
            prezzo.setPun(true);
            prezzo.setMaggiorazione(maggiorazione);
            prezzo.setSpread(spread);
        }
        return prezzoService.save(prezzo);
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

    public void importForniture() {
        List<FornituraModel> forniture = Poiji.fromExcel(new File("data/forniture.xlsx"), FornituraModel.class);
        for (FornituraModel fornituraModel : forniture) {
            Fornitura fornitura = new Fornitura();
            Cliente cliente = clienteService.findById(fornituraModel.getIdCliente());
            fornitura.setCliente(cliente);
            fornitura.setId(fornituraModel.getId());
            fornitura.setIndirizzo(fornituraModel.getIndirizzoFornitura());
            fornitura.setComune(fornituraModel.getComuneFornitura());
            fornitura.setProvincia(fornituraModel.getProvinciaFornitura());
            fornitura.setCap(fornituraModel.getCapFornitura());
            fornitura.setPotenzaDisponibile(fornituraModel.getPotenzaDisponibile());
            fornitura.setPotenzaImpegnata(fornituraModel.getPotenzaImpegnata());
            fornitura.setTipoPrelievo(TipoPrelievo.valueOf("BT"));
            if (fornituraModel.getTipoContatore().contains("asci")) {
                fornitura.setTipoContatore(TipoContatore.FASCIA);
            } else {
                fornitura.setTipoContatore(TipoContatore.ORARIO);
            }
            if (fornituraModel.getCodiceDistributore().contains("ENEL")) {
                fornitura.setCodiceDistributore(CodiceDistributore.EDIST);
            } else {
                fornitura.setCodiceDistributore(CodiceDistributore.A2A);
            }
            fornitura.setFornitore(fornituraModel.getFornitore());
            fornitura.setFatturazione(Fatturazione.MENSILE);
            Prezzo prezzo;
            Programmazione programmazione;
            if (fornituraModel.getDispacciamento().equals("10 ott")) {
                prezzo = createPrezzoIfNotExist("BASE " + fornituraModel.getSpread(), fornituraModel.getMaggiorazione(), fornituraModel.getSpread());
                programmazione = createProgrammazioneIfNotExist("BASE");
            } else {
                prezzo = createPrezzoIfNotExist("MAGGIORATO " + fornituraModel.getMaggiorazione(), fornituraModel.getMaggiorazione(), fornituraModel.getSpread());
                programmazione = createProgrammazioneIfNotExist("MAGGIORATO");
            }
            fornitura.setProgrammazione(programmazione);
            fornitura.setPrezzo(prezzo);
            fornitura.setBta(BTA.valueOf(fornituraModel.getBta()));
            fornitura.setIva(Double.parseDouble(fornituraModel.getIva()));
            fornitura.setDataSwitch(LocalDate.parse(fornituraModel.getDataSwitch()));

            fornituraService.save(fornitura);
        }

    }

    private Programmazione createProgrammazioneIfNotExist(String maggiorato) {
        Programmazione programmazione;
        try {
            programmazione = programmazioneService.findByNome(maggiorato);
        } catch (RuntimeException e) {
            programmazione = new Programmazione();
            programmazione.setNome(maggiorato);

            if (maggiorato.equals("BASE")) {
                programmazione.setOneriProgrammazione(14);
                programmazione.setCommercializzazione(10.47022);
            } else {
                programmazione.setOneriProgrammazione(8);
                programmazione.setCommercializzazione(15);
            }
        }
        return programmazioneService.save(programmazione);
    }
}
