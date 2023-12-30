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
        dispacciamento.setEolico(body.eolico());
        dispacciamento.setCostoAm(body.costoAm());
        dispacciamento.setDis(body.dis());
        dispacciamento.setInt73(body.int73());
        dispacciamento.setMsd(body.msd());
        dispacciamento.setSicurezza(body.sicurezza());
        dispacciamento.setTrasmissione(body.trasmissione());
        dispacciamento.setTrimestre(body.trimestre());
        dispacciamento.setAnno(body.anno());

        return dispacciamentoRepository.save(dispacciamento);
    }

    public Dispacciamento update(long id, DispacciamentoDTO body) {
        Dispacciamento dispacciamento = dispacciamentoRepository.findById(id).orElseThrow(() -> new NotFoundException("Dispacciamento non trovato"));
        dispacciamento.setCapacita(body.capacita());
        dispacciamento.setEolico(body.eolico());
        dispacciamento.setCostoAm(body.costoAm());
        dispacciamento.setDis(body.dis());
        dispacciamento.setInt73(body.int73());
        dispacciamento.setMsd(body.msd());
        dispacciamento.setSicurezza(body.sicurezza());
        dispacciamento.setTrasmissione(body.trasmissione());
        dispacciamento.setTrimestre(body.trimestre());
        dispacciamento.setAnno(body.anno());

        return dispacciamentoRepository.save(dispacciamento);
    }
}
