package org.emmek.IEG.services;

import org.emmek.IEG.entities.Dispacciamento;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.DispacciamentoDTO;
import org.emmek.IEG.repositories.DispacciamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispacciamentoService {

    @Autowired
    private DispacciamentoRepository dispacciamentoRepository;

    public List<Dispacciamento> findAll() {
        return dispacciamentoRepository.findAll();
    }

    public Dispacciamento findById(long id) {
        return dispacciamentoRepository.findById(id).orElseThrow(() -> new NotFoundException("Dispacciamento non trovato"));
    }

    public void delete(long id) {
        Dispacciamento dispacciamento = dispacciamentoRepository.findById(id).orElseThrow(() -> new NotFoundException("Dispacciamento non trovato"));
        dispacciamentoRepository.delete(dispacciamento);
    }

    public Dispacciamento save(DispacciamentoDTO body) {
        Dispacciamento dispacciamento = new Dispacciamento();
        dispacciamento.setCapacita(body.capacita());
        dispacciamento.setCostoAm(body.costoAm());
        dispacciamento.setDis(body.dis());
        dispacciamento.setAnno(body.anno());

        return dispacciamentoRepository.save(dispacciamento);
    }

    public Dispacciamento update(long id, DispacciamentoDTO body) {
        Dispacciamento dispacciamento = dispacciamentoRepository.findById(id).orElseThrow(() -> new NotFoundException("Dispacciamento non trovato"));
        dispacciamento.setCapacita(body.capacita());
        dispacciamento.setCostoAm(body.costoAm());
        dispacciamento.setDis(body.dis());
        dispacciamento.setAnno(body.anno());

        return dispacciamentoRepository.save(dispacciamento);
    }

    public Dispacciamento findByTrimestreAndAnno(int mese, Integer anno) {
        return dispacciamentoRepository.findByMeseAndAnno(mese, anno).orElseThrow(() -> new NotFoundException("Dispacciamento trimestre: " + mese + " - anno: " + anno + "non trovato"));
    }
}
