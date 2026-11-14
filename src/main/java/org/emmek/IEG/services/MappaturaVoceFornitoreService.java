package org.emmek.IEG.services;

import org.emmek.IEG.entities.MappaturaVoceFornitore;
import org.emmek.IEG.entities.VoceFatturaFornitore;
import org.emmek.IEG.enums.VoceConfronto;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.MappaturaVoceFornitoreDTO;
import org.emmek.IEG.payloads.TariffaNonMappataDTO;
import org.emmek.IEG.repositories.MappaturaVoceFornitoreRepository;
import org.emmek.IEG.repositories.VoceFatturaFornitoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class MappaturaVoceFornitoreService {

    @Autowired
    private MappaturaVoceFornitoreRepository mappaturaRepository;

    @Autowired
    private VoceFatturaFornitoreRepository voceFatturaFornitoreRepository;

    public List<MappaturaVoceFornitore> findAll() {
        return mappaturaRepository.findAllByOrderByVoceConfrontoAscChiaveAsc();
    }

    public MappaturaVoceFornitore findById(Long id) {
        return mappaturaRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    public MappaturaVoceFornitore save(MappaturaVoceFornitoreDTO body) {
        MappaturaVoceFornitore mappatura = new MappaturaVoceFornitore();
        copia(body, mappatura);
        return mappaturaRepository.save(mappatura);
    }

    public MappaturaVoceFornitore update(Long id, MappaturaVoceFornitoreDTO body) {
        MappaturaVoceFornitore mappatura = findById(id);
        copia(body, mappatura);
        return mappaturaRepository.save(mappatura);
    }

    public void delete(Long id) {
        mappaturaRepository.delete(findById(id));
    }

    /**
     * Tariffe delle fatture del fornitore importate che nessuna mappatura riconosce.
     */
    public List<TariffaNonMappataDTO> findNonMappate() {
        Mappatore mappatore = mappatore();
        return voceFatturaFornitoreRepository.findTariffeDistinte().stream()
                .map(r -> new TariffaNonMappataDTO((String) r[0], (String) r[1], (String) r[2]))
                .filter(t -> mappatore.regola(t.codTariffa(), t.nrArticolo(), t.descrizione()) == null)
                .sorted(Comparator.comparing(TariffaNonMappataDTO::codTariffa, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(TariffaNonMappataDTO::nrArticolo, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Mappatore con le regole attuali, da usare per tutte le righe di un'elaborazione.
     */
    public Mappatore mappatore() {
        return new Mappatore(mappaturaRepository.findAll());
    }

    private void copia(MappaturaVoceFornitoreDTO body, MappaturaVoceFornitore mappatura) {
        mappatura.setChiave(body.chiave().trim());
        mappatura.setPerdite(body.perdite());
        mappatura.setVoceConfronto(VoceConfronto.valueOf(body.voceConfronto()));
        mappatura.setQuantita(body.quantita());
    }

    public static class Mappatore {
        // regole esatte prima, poi i prefissi dal più lungo
        private final List<MappaturaVoceFornitore> regole;

        Mappatore(List<MappaturaVoceFornitore> regole) {
            this.regole = regole.stream()
                    .sorted(Comparator.comparing(MappaturaVoceFornitore::isPrefisso)
                            .thenComparing(m -> -m.getChiave().length()))
                    .toList();
        }

        public MappaturaVoceFornitore regola(VoceFatturaFornitore riga) {
            return regola(riga.getCodTariffa(), riga.getNrArticolo(), riga.getDescrizione());
        }

        /**
         * Regola per la riga: si cerca per cod tariffa e, se non ce n'è (o la tariffa è vuota), per nr articolo.
         * Per le righe "Perdite ..." vale prima la regola con perdite = true, poi quella senza.
         * null se nessuna regola si applica.
         */
        public MappaturaVoceFornitore regola(String codTariffa, String nrArticolo, String descrizione) {
            boolean perdite = descrizione != null && descrizione.toLowerCase().contains("perdite");
            for (String valore : new String[]{codTariffa, nrArticolo}) {
                if (valore == null || valore.isBlank()) continue;
                MappaturaVoceFornitore regola = cerca(valore, perdite);
                if (regola == null && perdite) regola = cerca(valore, false);
                if (regola != null) return regola;
            }
            return null;
        }

        public VoceConfronto voce(VoceFatturaFornitore riga) {
            MappaturaVoceFornitore regola = regola(riga);
            return regola != null ? regola.getVoceConfronto() : VoceConfronto.NON_MAPPATA;
        }

        private MappaturaVoceFornitore cerca(String valore, boolean perdite) {
            return regole.stream()
                    .filter(m -> m.isPerdite() == perdite && m.corrisponde(valore))
                    .findFirst()
                    .orElse(null);
        }
    }
}
