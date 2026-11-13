package org.emmek.IEG.services;

import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Pun;
import org.emmek.IEG.repositories.PunPodRepository;
import org.emmek.IEG.repositories.PunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PunService {

    @Autowired
    private PunRepository punRepository;

    @Autowired
    private PunPodRepository punPodRepository;

    public Pun getByMeseAndAnno(int mese, int anno) {
        return punRepository.findByMeseAndAnno(mese, anno).orElseThrow(() -> new RuntimeException("Pun non trovato mese: " + mese + " - anno: " + anno));
    }

    /**
     * PUN da applicare alla fornitura nel mese: quello specifico del POD (tabella pun_pod) se presente,
     * altrimenti quello generico del mese (tabella pun).
     */
    public Pun getPun(Fornitura fornitura, int mese, int anno) {
        return punPodRepository.findByFornituraAndMeseAndAnno(fornitura, mese, anno)
                .map(p -> new Pun(null, p.getF1(), p.getF2(), p.getF3(), mese, anno))
                .orElseGet(() -> getByMeseAndAnno(mese, anno));
    }
}
