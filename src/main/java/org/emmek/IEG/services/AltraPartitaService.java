package org.emmek.IEG.services;

import org.emmek.IEG.entities.AltraPartita;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.AltraPartitaDTO;
import org.emmek.IEG.repositories.AltraPartitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AltraPartitaService {

    @Autowired
    private AltraPartitaRepository altraPartitaRepository;

    @Autowired
    private FornituraService fornituraService;

    /**
     * Altre partite filtrate per pod, mese e anno (filtri opzionali), ordinate per anno, mese, id.
     */
    public List<AltraPartita> findAll(String pod, Integer mese, Integer anno) {
        Specification<AltraPartita> spec = (root, query, cb) -> cb.conjunction();
        if (pod != null && !pod.isBlank()) {
            String podTrim = pod.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fornitura").get("id"), podTrim));
        }
        if (mese != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mese"), mese));
        }
        if (anno != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("anno"), anno));
        }
        return altraPartitaRepository.findAll(spec, Sort.by("anno", "mese", "id"));
    }

    public AltraPartita findById(Long id) {
        return altraPartitaRepository.findById(id).orElseThrow(() -> new NotFoundException("Altra partita con id: " + id + " non trovata"));
    }

    public AltraPartita save(AltraPartitaDTO body) {
        AltraPartita altraPartita = new AltraPartita();
        copia(body, altraPartita);
        return altraPartitaRepository.save(altraPartita);
    }

    public AltraPartita update(Long id, AltraPartitaDTO body) {
        AltraPartita altraPartita = findById(id);
        copia(body, altraPartita);
        return altraPartitaRepository.save(altraPartita);
    }

    /**
     * Attiva o disattiva la partita: quelle disattivate restano in archivio ma non vanno in fattura.
     */
    public AltraPartita setAttiva(Long id, boolean attiva) {
        AltraPartita altraPartita = findById(id);
        altraPartita.setAttiva(attiva);
        return altraPartitaRepository.save(altraPartita);
    }

    public void delete(Long id) {
        altraPartitaRepository.delete(findById(id));
    }

    private void copia(AltraPartitaDTO body, AltraPartita altraPartita) {
        altraPartita.setFornitura(fornituraService.findById(body.pod().trim()));
        altraPartita.setVoce(body.voce());
        altraPartita.setImporto(body.importo());
        altraPartita.setEsenteIva(body.esenteIva() == null || body.esenteIva());
        altraPartita.setMese(body.mese());
        altraPartita.setAnno(body.anno());
    }
}
