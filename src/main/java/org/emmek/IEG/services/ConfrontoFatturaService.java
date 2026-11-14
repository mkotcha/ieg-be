package org.emmek.IEG.services;

import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.VoceConfronto;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.ConfrontoFatturaDTO;
import org.emmek.IEG.payloads.ConfrontoFornituraDTO;
import org.emmek.IEG.payloads.ConfrontoMeseDTO;
import org.emmek.IEG.payloads.ConfrontoVoceDTO;
import org.emmek.IEG.repositories.FatturaRepository;
import org.emmek.IEG.repositories.FornituraRepository;
import org.emmek.IEG.repositories.VoceFatturaFornitoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.emmek.IEG.enums.VoceConfronto.*;

/**
 * Confronto delle nostre fatture con le fatture del fornitore: le voci di entrambe sono ricondotte alle
 * {@link VoceConfronto} (le nostre per codice, quelle del fornitore con le mappature in DB) e sommate per
 * fornitura e mese di competenza.
 */
@Service
public class ConfrontoFatturaService {

    // tolleranza sulle voci passanti: 10 centesimi o l'1% dell'importo del fornitore
    private static final double TOLLERANZA_IMPORTO = 0.10;
    private static final double TOLLERANZA_IMPORTO_PERC = 0.01;
    // il fornitore arrotonda i kWh all'unità per fascia
    private static final double TOLLERANZA_KWH = 1;

    /**
     * Nostre voci (codice di VoceFatturaSingola) → voce di confronto. Le voci in VOCI_CON_QUANTITA concorrono alla
     * quantità (es. dispacciamento + perdite dispacciamento = kWh con perdite, come la riga del fornitore).
     */
    private static final Map<String, VoceConfronto> VOCI_NOSTRE = Map.ofEntries(
            Map.entry("energia_f1", ENERGIA_F1),
            Map.entry("energia_f2", ENERGIA_F2),
            Map.entry("energia_f3", ENERGIA_F3),
            Map.entry("perdite_f1", PERDITE_F1),
            Map.entry("perdite_f2", PERDITE_F2),
            Map.entry("perdite_f3", PERDITE_F3),
            Map.entry("spread", COMMERCIALIZZAZIONE),
            Map.entry("commercializzazione", COMMERCIALIZZAZIONE),
            Map.entry("ocv", COMMERCIALIZZAZIONE),
            Map.entry("rettifica", COMMERCIALIZZAZIONE),
            Map.entry("dispacciamento", DISPACCIAMENTO),
            Map.entry("perdite_dispacciamento", DISPACCIAMENTO),
            Map.entry("maggiorazione_capacita", MAGGIORAZIONE_CAPACITA),
            Map.entry("perdite_maggiorazione_capacita", MAGGIORAZIONE_CAPACITA),
            Map.entry("aggregazione", QUOTE_FISSE_DISPACCIAMENTO),
            Map.entry("qf_dbt", QUOTE_FISSE_DISPACCIAMENTO),
            Map.entry("qe_tud", DISTRIBUZIONE),
            Map.entry("trasmissione", TRASMISSIONE),
            Map.entry("qf_tud", QUOTE_FISSE_RETE),
            Map.entry("qf_mis", QUOTE_FISSE_RETE),
            Map.entry("qp_tdm", QUOTE_POTENZA_RETE),
            Map.entry("qe_uc3", UC3),
            Map.entry("qe_uc6", UC6),
            Map.entry("qf_uc6", UC6),
            Map.entry("qe_asos", ASOS_ENERGIA),
            Map.entry("qf_asos", ASOS_FISSA),
            Map.entry("qp_asos", ASOS_POTENZA),
            Map.entry("qe_arim", ARIM_ENERGIA),
            Map.entry("qf_arim", ARIM_FISSA),
            Map.entry("qp_arim", ARIM_POTENZA),
            Map.entry("reattiva_33", REATTIVA_33),
            Map.entry("reattiva_75", REATTIVA_75),
            Map.entry("reattiva_immessa", REATTIVA_IMMESSA),
            Map.entry("addizionale_erariale", ACCISE)
    );

    private static final Set<String> VOCI_CON_QUANTITA = Set.of(
            "energia_f1", "energia_f2", "energia_f3", "perdite_f1", "perdite_f2", "perdite_f3",
            "dispacciamento", "perdite_dispacciamento", "qe_tud", "trasmissione", "qp_tdm", "qe_uc3", "qe_uc6",
            "qe_asos", "qp_asos", "qe_arim", "qp_arim", "reattiva_33", "reattiva_75", "reattiva_immessa",
            "addizionale_erariale");

    private static final Set<VoceConfronto> VOCI_KWH = EnumSet.of(ENERGIA_F1, ENERGIA_F2, ENERGIA_F3,
            PERDITE_F1, PERDITE_F2, PERDITE_F3);

    @Autowired
    private FatturaRepository fatturaRepository;

    @Autowired
    private VoceFatturaFornitoreRepository voceFatturaFornitoreRepository;

    @Autowired
    private FornituraRepository fornituraRepository;

    @Autowired
    private MappaturaVoceFornitoreService mappaturaService;

    /**
     * Confronto di dettaglio di una fattura: per fornitura e voce.
     */
    @Transactional(readOnly = true)
    public ConfrontoFatturaDTO confronta(String numeroFattura) {
        Fattura fattura = fatturaRepository.findByNumeroFattura(numeroFattura)
                .orElseThrow(() -> new NotFoundException("Fattura " + numeroFattura + " non trovata"));
        List<String> pod = fattura.getFattureSingole().stream().map(fs -> fs.getFornitura().getId()).toList();
        Map<String, List<VoceFatturaFornitore>> righe = pod.isEmpty() ? Map.of()
                : perPod(voceFatturaFornitoreRepository.findByCompetenzaAndPod(fattura.getMese(), fattura.getAnno(), pod));
        return confronta(fattura, righe, mappaturaService.mappatore(), true);
    }

    /**
     * Riepilogo del confronto di tutte le fatture emesse per il mese di consumo (senza il dettaglio per fornitura).
     */
    @Transactional(readOnly = true)
    public ConfrontoMeseDTO confrontaMese(int mese, int anno) {
        MappaturaVoceFornitoreService.Mappatore mappatore = mappaturaService.mappatore();
        List<VoceFatturaFornitore> tutte = voceFatturaFornitoreRepository.findByCompetenza(mese, anno);
        Map<String, List<VoceFatturaFornitore>> righe = perPod(tutte);

        List<ConfrontoFatturaDTO> fatture = new ArrayList<>();
        Set<String> podFatturati = new HashSet<>();
        for (Fattura fattura : fatturaRepository.findByMeseAndAnnoOrderByNumeroFattura(mese, anno)) {
            fattura.getFattureSingole().forEach(fs -> podFatturati.add(fs.getFornitura().getId()));
            fatture.add(confronta(fattura, righe, mappatore, false));
        }

        double costiIeg = tutte.stream().filter(r -> mappatore.voce(r) == COSTI_IEG)
                .mapToDouble(VoceFatturaFornitore::getImporto).sum();
        Set<String> podAnagrafica = new HashSet<>(fornituraRepository.getListaPod());
        List<ConfrontoMeseDTO.PodSenzaFatturaDTO> podSenzaFattura = righe.entrySet().stream()
                .filter(e -> !podFatturati.contains(e.getKey()))
                .map(e -> new ConfrontoMeseDTO.PodSenzaFatturaDTO(e.getKey(), podAnagrafica.contains(e.getKey()),
                        arrotonda(e.getValue().stream().filter(r -> mappatore.voce(r) != COSTI_IEG)
                                .mapToDouble(VoceFatturaFornitore::getImporto).sum())))
                .filter(p -> p.importoFornitore() != 0)
                .sorted(Comparator.comparing(ConfrontoMeseDTO.PodSenzaFatturaDTO::pod))
                .toList();

        return new ConfrontoMeseDTO(mese, anno, !tutte.isEmpty(),
                arrotonda(fatture.stream().mapToDouble(ConfrontoFatturaDTO::totaleNostro).sum()),
                arrotonda(fatture.stream().mapToDouble(ConfrontoFatturaDTO::totaleFornitore).sum()),
                arrotonda(fatture.stream().mapToDouble(ConfrontoFatturaDTO::margine).sum()),
                arrotonda(fatture.stream().mapToDouble(ConfrontoFatturaDTO::differenzaPassanti).sum()),
                arrotonda(costiIeg), fatture, podSenzaFattura);
    }

    private ConfrontoFatturaDTO confronta(Fattura fattura, Map<String, List<VoceFatturaFornitore>> righe,
                                          MappaturaVoceFornitoreService.Mappatore mappatore, boolean dettaglio) {
        List<ConfrontoFornituraDTO> forniture = fattura.getFattureSingole().stream()
                .sorted(Comparator.comparing(fs -> fs.getFornitura().getId()))
                .map(fs -> confronta(fs, righe.getOrDefault(fs.getFornitura().getId(), List.of()), mappatore))
                .toList();
        Cliente cliente = fattura.getCliente();
        return new ConfrontoFatturaDTO(fattura.getNumeroFattura(), cliente.getId(), cliente.getRagioneSociale(),
                fattura.getMese(), fattura.getAnno(),
                arrotonda(forniture.stream().mapToDouble(ConfrontoFornituraDTO::totaleNostro).sum()),
                arrotonda(forniture.stream().mapToDouble(ConfrontoFornituraDTO::totaleFornitore).sum()),
                arrotonda(forniture.stream().mapToDouble(ConfrontoFornituraDTO::margine).sum()),
                arrotonda(forniture.stream().mapToDouble(ConfrontoFornituraDTO::differenzaPassanti).sum()),
                forniture.stream().mapToInt(f -> f.anomalie().size()).sum(),
                dettaglio ? forniture : null);
    }

    private ConfrontoFornituraDTO confronta(FatturaSingola fs, List<VoceFatturaFornitore> righeFornitore,
                                            MappaturaVoceFornitoreService.Mappatore mappatore) {
        Fornitura fornitura = fs.getFornitura();
        Map<VoceConfronto, Somma> somme = new EnumMap<>(VoceConfronto.class);
        for (VoceFatturaSingola voce : fs.getVoci()) {
            VoceConfronto vc = voce.getCodice().startsWith("altra_partita_") ? ALTRE_PARTITE
                    : VOCI_NOSTRE.getOrDefault(voce.getCodice(), NON_MAPPATA);
            somme.computeIfAbsent(vc, k -> new Somma())
                    .nostra(nvl(voce.getImporto()), VOCI_CON_QUANTITA.contains(voce.getCodice()) ? voce.getQuantita() : null);
        }
        List<VoceFatturaFornitore> righe = new ArrayList<>();
        Set<String> nonMappate = new TreeSet<>();
        for (VoceFatturaFornitore riga : righeFornitore) {
            MappaturaVoceFornitore regola = mappatore.regola(riga);
            VoceConfronto vc = regola != null ? regola.getVoceConfronto() : NON_MAPPATA;
            if (vc == COSTI_IEG) continue;
            righe.add(riga);
            if (regola == null) nonMappate.add(riga.getDescrizione());
            somme.computeIfAbsent(vc, k -> new Somma())
                    .fornitore(riga.getImporto(), regola != null && regola.isQuantita() ? riga.getQuantita() : null);
        }

        List<String> anomalie = new ArrayList<>();
        if (righe.isEmpty()) anomalie.add("Nessuna riga del fornitore per il mese");
        List<ConfrontoVoceDTO> voci = new ArrayList<>();
        double totaleNostro = 0, totaleFornitore = 0, margine = 0, differenzaPassanti = 0;
        for (Map.Entry<VoceConfronto, Somma> e : somme.entrySet()) {
            VoceConfronto vc = e.getKey();
            Somma s = e.getValue();
            totaleNostro += s.importoNostro;
            totaleFornitore += s.importoFornitore;
            if (vc.concorreAlMargine()) margine += s.importoNostro - s.importoFornitore;
            if (s.vuota()) continue;
            double diffImporto = s.importoNostro - s.importoFornitore;
            Double diffQuantita = s.quantitaNostra != null && s.quantitaFornitore != null
                    ? s.quantitaNostra - s.quantitaFornitore : null;
            boolean anomala = false;
            if (vc.isPassante()) {
                differenzaPassanti += diffImporto;
                anomala = !righe.isEmpty() && Math.abs(diffImporto)
                        > Math.max(TOLLERANZA_IMPORTO, TOLLERANZA_IMPORTO_PERC * Math.abs(s.importoFornitore));
            } else if (VOCI_KWH.contains(vc)) {
                anomala = !righe.isEmpty() && Math.abs(nvl(s.quantitaNostra) - nvl(s.quantitaFornitore)) > TOLLERANZA_KWH;
            }
            if (anomala) {
                anomalie.add(vc.getDescrizione() + ": differenza "
                        + (VOCI_KWH.contains(vc) ? arrotonda(nvl(diffQuantita)) + " kWh" : arrotonda(diffImporto) + " €"));
            }
            voci.add(new ConfrontoVoceDTO(vc, vc.getDescrizione(), vc.isPassante(),
                    arrotonda(s.quantitaNostra), arrotonda(s.importoNostro),
                    arrotonda(s.quantitaFornitore), arrotonda(s.importoFornitore),
                    arrotonda(diffQuantita), arrotonda(diffImporto), anomala));
        }
        if (!nonMappate.isEmpty()) anomalie.add("Righe del fornitore non mappate: " + String.join(", ", nonMappate));

        // anagrafica: BTA e potenza impegnata dell'ultima riga ordinaria (non di ricalcolo) del fornitore
        Optional<VoceFatturaFornitore> ultima = righe.stream()
                .filter(r -> !r.isRicalcolo() && r.getFinePeriodo() != null)
                .max(Comparator.comparing(VoceFatturaFornitore::getFinePeriodo));
        String bta = fornitura.getBta() != null ? fornitura.getBta().name() : null;
        String btaFornitore = ultima.map(VoceFatturaFornitore::getClasseUtenza).map(c -> c.equals("D4") ? "TD" : c).orElse(null);
        Double potenzaFornitore = ultima.map(VoceFatturaFornitore::getPotenzaImpegnata).orElse(null);
        if (btaFornitore != null && !btaFornitore.equals(bta)) {
            anomalie.add("BTA diversa: nostra " + bta + ", fornitore " + btaFornitore);
        }
        if (potenzaFornitore != null && fornitura.getPotenzaImpegnata() != null
                && Math.abs(potenzaFornitore - fornitura.getPotenzaImpegnata()) > 0.01) {
            anomalie.add("Potenza impegnata diversa: nostra " + fornitura.getPotenzaImpegnata() + " kW, fornitore "
                    + potenzaFornitore + " kW");
        }

        List<String> bollette = righe.stream().map(VoceFatturaFornitore::getNumeroFattura).distinct().sorted().toList();
        return new ConfrontoFornituraDTO(fornitura.getId(), bta, btaFornitore, fornitura.getPotenzaImpegnata(),
                potenzaFornitore, bollette, righe.stream().anyMatch(VoceFatturaFornitore::isRicalcolo),
                arrotonda(totaleNostro), arrotonda(totaleFornitore), arrotonda(margine), arrotonda(differenzaPassanti),
                anomalie, voci);
    }

    private static Map<String, List<VoceFatturaFornitore>> perPod(List<VoceFatturaFornitore> righe) {
        return righe.stream().filter(r -> r.getPod() != null)
                .collect(Collectors.groupingBy(VoceFatturaFornitore::getPod));
    }

    private static double nvl(Double d) {
        return d != null ? d : 0;
    }

    private static Double arrotonda(Double valore) {
        return valore == null ? null : Math.round(valore * 100) / 100.0;
    }

    private static double arrotonda(double valore) {
        return Math.round(valore * 100) / 100.0;
    }

    private static class Somma {
        Double quantitaNostra;
        double importoNostro;
        Double quantitaFornitore;
        double importoFornitore;

        void nostra(double importo, Double quantita) {
            importoNostro += importo;
            if (quantita != null) quantitaNostra = nvl(quantitaNostra) + quantita;
        }

        void fornitore(double importo, Double quantita) {
            importoFornitore += importo;
            if (quantita != null) quantitaFornitore = nvl(quantitaFornitore) + quantita;
        }

        boolean vuota() {
            return Math.abs(importoNostro) < 0.005 && Math.abs(importoFornitore) < 0.005
                    && Math.abs(nvl(quantitaNostra)) < 0.005 && Math.abs(nvl(quantitaFornitore)) < 0.005;
        }
    }
}
