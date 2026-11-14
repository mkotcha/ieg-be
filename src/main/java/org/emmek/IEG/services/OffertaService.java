package org.emmek.IEG.services;

import org.emmek.IEG.entities.Offerta;
import org.emmek.IEG.enums.ModalitaProgrammazione;
import org.emmek.IEG.enums.TipoOfferta;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.OffertaDTO;
import org.emmek.IEG.repositories.OffertaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OffertaService {

    @Autowired
    private OffertaRepository offertaRepository;

    public List<Offerta> findAll() {
        return offertaRepository.findAll(Sort.by("id"));
    }

    public Offerta findByNome(String nome) {
        return offertaRepository.findByNome(nome).orElseThrow(() -> new RuntimeException("Prezzo con nome: " + nome + " non trovato"));
    }

    public Offerta save(Offerta offerta) {
        return offertaRepository.save(offerta);
    }

    public Offerta findById(Long id) {
        return offertaRepository.findById(id).orElseThrow(() -> new NotFoundException("Prezzo con id: " + id + " non trovato"));
    }

    public Offerta save(OffertaDTO body) {
        Offerta offerta = new Offerta();
        copia(body, offerta);
        return offertaRepository.save(offerta);
    }

    public Offerta update(Long id, OffertaDTO body) {
        Offerta offerta = findById(id);
        copia(body, offerta);
        return offertaRepository.save(offerta);
    }

    /**
     * Elimina l'offerta solo se nessuna fornitura la usa.
     */
    public void delete(Long id) {
        Offerta offerta = findById(id);
        long forniture = offertaRepository.countForniture(id);
        if (forniture > 0) {
            throw new BadRequestException("Offerta con id: " + id + " usata da " + forniture + " forniture, impossibile eliminarla");
        }
        offertaRepository.delete(offerta);
    }

    private void copia(OffertaDTO body, Offerta offerta) {
        offerta.setNome(body.nome());
        offerta.setCodice(body.codice());
        offerta.setTipo(body.tipo() != null ? TipoOfferta.valueOf(body.tipo()) : null);
        offerta.setF1(body.f1());
        offerta.setF2(body.f2());
        offerta.setF3(body.f3());
        offerta.setPun(body.pun());
        offerta.setCapacitaMaggiorazione(body.capacitaMaggiorazione());
        offerta.setMaggiorazione(body.maggiorazione());
        offerta.setHasSpread(body.hasSpread());
        offerta.setSpread(body.spread());
        offerta.setOcv(body.ocv());
        offerta.setModalitaProgrammazione(body.modalitaProgrammazione() != null
                ? ModalitaProgrammazione.valueOf(body.modalitaProgrammazione())
                : ModalitaProgrammazione.OCV);
        offerta.setOcf(body.ocf());
        offerta.setDescrizione(body.descrizione());
        offerta.setDataInizio(body.dataInizio());
        offerta.setDataFine(body.dataFine());
    }
}
