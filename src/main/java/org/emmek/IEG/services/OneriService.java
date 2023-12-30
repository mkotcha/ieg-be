package org.emmek.IEG.services;

import org.emmek.IEG.entities.Oneri;
import org.emmek.IEG.enums.BTA;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.OneriDTO;
import org.emmek.IEG.repositories.OneriRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OneriService {

    @Autowired
    private OneriRepository oneriRepository;

    public List<Oneri> findAll() {
        return oneriRepository.findAll();
    }

    public Oneri save(OneriDTO body) {
        Oneri oneri = new Oneri();
        oneri.setTipo(BTA.valueOf(body.tipo()));
        oneri.setQeTud(body.qeTud());
        oneri.setQpTdm(body.qpTdm());
        oneri.setQfTud(body.qfTud());
        oneri.setQfMis(body.qfMis());
        oneri.setQeArim(body.qeArim());
        oneri.setQeAsos(body.qeAsos());
        oneri.setQeUc3(body.qeUc3());
        oneri.setQpArim(body.qpArim());
        oneri.setQpAsos(body.qpAsos());
        oneri.setQpOds(body.qpOds());
        oneri.setQfArim(body.qfArim());
        oneri.setQfAsos(body.qfAsos());
        oneri.setTrimestre(body.trimestre());
        oneri.setAnno(body.anno());

        return oneriRepository.save(oneri);
    }

    public Oneri update(long id, OneriDTO body) {
        Oneri oneri = oneriRepository.findById(id).orElseThrow(() -> new NotFoundException("Voce non trovata"));
        oneri.setTipo(BTA.valueOf(body.tipo()));
        oneri.setQeTud(body.qeTud());
        oneri.setQpTdm(body.qpTdm());
        oneri.setQfTud(body.qfTud());
        oneri.setQfMis(body.qfMis());
        oneri.setQeArim(body.qeArim());
        oneri.setQeAsos(body.qeAsos());
        oneri.setQeUc3(body.qeUc3());
        oneri.setQpArim(body.qpArim());
        oneri.setQpAsos(body.qpAsos());
        oneri.setQpOds(body.qpOds());
        oneri.setQfArim(body.qfArim());
        oneri.setQfAsos(body.qfAsos());
        oneri.setTrimestre(body.trimestre());
        oneri.setAnno(body.anno());

        return oneriRepository.save(oneri);


    }

    public void delete(long id) {
        Oneri oneri = oneriRepository.findById(id).orElseThrow(() -> new NotFoundException("Voce non trovata"));
        oneriRepository.delete(oneri);
    }

    public Oneri findById(long id) {
        return oneriRepository.findById(id).orElseThrow(() -> new NotFoundException("Voce non trovata"));
    }
}
