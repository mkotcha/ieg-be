package org.emmek.IEG.services;

import org.emmek.IEG.entities.Dispacciamento;
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
}
