package org.emmek.IEG.services;

import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.enums.*;
import org.emmek.IEG.payloads.FornituraDTO;
import org.emmek.IEG.repositories.FornituraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FornituraService {

    @Autowired
    private FornituraRepository fornituraRepository;

//    @Autowired
//    private ClienteService clienteService;

    @Autowired
    private PrezzoService prezzoService;

    @Autowired
    private ProgrammazioneService programmazioneService;

    public Page<Fornitura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return fornituraRepository.findAll(pageable);
    }

    public void save(Fornitura fornitura) {
        fornituraRepository.save(fornitura);
    }

    public Fornitura finById(String pod) {
        return fornituraRepository.findById(pod).orElseThrow(() -> new RuntimeException("Fornitura con id: " + pod + " non trovata"));
    }

    public List<String> getListaPod() {
        return fornituraRepository.getListaPod();
    }

    public Fornitura findById(String id) {
        return fornituraRepository.findById(id).orElseThrow(() -> new RuntimeException("Fornitura con id: " + id + " non trovata"));
    }

    public List<Fornitura> findByCliente(Cliente cliente) {
        return fornituraRepository.findByCliente(cliente);
    }

    public void delete(String id) {
        Fornitura fornitura = fornituraRepository.findById(id).orElseThrow(() -> new RuntimeException("Fornitura con id: " + id + " non trovata"));
        fornituraRepository.delete(fornitura);
    }

    public Fornitura setFornitura(FornituraDTO payload, Cliente cliente) {
        Fornitura fornitura = new Fornitura();
        fornitura.setCliente(cliente);
        fornitura.setPrezzo(prezzoService.findById(Long.valueOf(payload.idPrezzo())));
        fornitura.setProgrammazione(programmazioneService.findById(Long.valueOf(payload.idProgrammazione())));
        fornitura.setBta(BTA.valueOf(payload.bta()));
        fornitura.setCap(Integer.parseInt(payload.cap()));
        fornitura.setCodiceDistributore(CodiceDistributore.valueOf(payload.codiceDistributore()));
        fornitura.setComune(payload.comune());
        fornitura.setDataSwitch(LocalDate.parse(payload.dataSwitch()));
        fornitura.setDataSwitchOut(LocalDate.parse(payload.dataSwitchOut()));
        fornitura.setFatturazione(Fatturazione.valueOf(payload.fatturazione()));
        fornitura.setFornitore(payload.fornitore());
        fornitura.setIndirizzo(payload.indirizzo());
        fornitura.setIva(Double.parseDouble(payload.iva()));
        fornitura.setPotenzaDisponibile(Double.parseDouble(payload.potenzaDisponibile()));
        fornitura.setPotenzaImpegnata(Double.parseDouble(payload.potenzaImpegnata()));
        fornitura.setProvincia(payload.provincia());
        fornitura.setTipoContatore(TipoContatore.valueOf(payload.tipoContatore()));
        fornitura.setTipoPrelievo(TipoPrelievo.valueOf(payload.tipoPrelievo()));
        return fornituraRepository.save(fornitura);
    }

    public Fornitura updateFornitura(String id, FornituraDTO payload, Cliente cliente) {
        Fornitura fornitura = findById(id);
        fornitura.setCliente(cliente);
        fornitura.setPrezzo(prezzoService.findById(Long.valueOf(payload.idPrezzo())));
        fornitura.setProgrammazione(programmazioneService.findById(Long.valueOf(payload.idProgrammazione())));
        fornitura.setBta(BTA.valueOf(payload.bta()));
        fornitura.setCap(Integer.parseInt(payload.cap()));
        fornitura.setCodiceDistributore(CodiceDistributore.valueOf(payload.codiceDistributore()));
        fornitura.setComune(payload.comune());
        fornitura.setDataSwitch(LocalDate.parse(payload.dataSwitch()));
        fornitura.setDataSwitchOut(LocalDate.parse(payload.dataSwitchOut()));
        fornitura.setFatturazione(Fatturazione.valueOf(payload.fatturazione()));
        fornitura.setFornitore(payload.fornitore());
        fornitura.setIndirizzo(payload.indirizzo());
        fornitura.setIva(Double.parseDouble(payload.iva()));
        fornitura.setPotenzaDisponibile(Double.parseDouble(payload.potenzaDisponibile()));
        fornitura.setPotenzaImpegnata(Double.parseDouble(payload.potenzaImpegnata()));
        fornitura.setProvincia(payload.provincia());
        fornitura.setTipoContatore(TipoContatore.valueOf(payload.tipoContatore()));
        fornitura.setTipoPrelievo(TipoPrelievo.valueOf(payload.tipoPrelievo()));
        return fornituraRepository.save(fornitura);
    }

}
