package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.FatturaFornitore;
import org.emmek.IEG.entities.VoceFatturaFornitore;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.EsitoImportFatturaFornitoreDTO;
import org.emmek.IEG.repositories.FatturaFornitoreRepository;
import org.emmek.IEG.repositories.FornituraRepository;
import org.emmek.IEG.repositories.VoceFatturaFornitoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Import dei csv di riepilogo delle fatture del fornitore (Fatt_&lt;numero&gt;_&lt;data&gt;.csv, anche zippati):
 * separatore ';', UTF-8, decimali con la virgola, una riga per voce per POD.
 */
@Service
@Slf4j
public class FatturaFornitoreService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private FatturaFornitoreRepository fatturaFornitoreRepository;

    @Autowired
    private VoceFatturaFornitoreRepository voceFatturaFornitoreRepository;

    @Autowired
    private FornituraRepository fornituraRepository;

    @Autowired
    private MappaturaVoceFornitoreService mappaturaService;

    public List<FatturaFornitore> findAll() {
        return fatturaFornitoreRepository.findAllByOrderByDataEmissioneDesc();
    }

    public FatturaFornitore findByNumero(String numero) {
        return fatturaFornitoreRepository.findByNumero(numero)
                .orElseThrow(() -> new NotFoundException("Fattura fornitore " + numero + " non trovata"));
    }

    @Transactional
    public void delete(String numero) {
        FatturaFornitore fattura = findByNumero(numero);
        voceFatturaFornitoreRepository.deleteByFatturaFornitore(fattura);
        fatturaFornitoreRepository.delete(fattura);
    }

    /**
     * Righe delle fatture del fornitore filtrate per bolletta, pod e competenza (filtri opzionali).
     */
    public List<VoceFatturaFornitore> findVoci(String numero, String pod, Integer mese, Integer anno) {
        Specification<VoceFatturaFornitore> spec = (root, query, cb) -> cb.conjunction();
        if (numero != null && !numero.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fatturaFornitore").get("numero"), numero.trim()));
        }
        if (pod != null && !pod.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pod"), pod.trim()));
        }
        if (mese != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mese"), mese));
        }
        if (anno != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("anno"), anno));
        }
        return voceFatturaFornitoreRepository.findAll(spec, Sort.by("fatturaFornitore.id", "riga"));
    }

    /**
     * Importa un csv o uno zip di csv. Una bolletta già importata viene sostituita (le sue righe sono cancellate e
     * reinserite). Le righe non leggibili sono registrate nel log e saltate.
     */
    @Transactional
    public List<EsitoImportFatturaFornitoreDTO> upload(MultipartFile file) throws IOException {
        String nome = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        List<EsitoImportFatturaFornitoreDTO> esiti = new ArrayList<>();
        if (nome.toLowerCase().endsWith(".zip")) {
            try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String nomeEntry = new java.io.File(entry.getName()).getName();
                    if (!entry.isDirectory() && nomeEntry.toLowerCase().endsWith(".csv")) {
                        esiti.addAll(importaCsv(nomeEntry, zip.readAllBytes()));
                    }
                }
            }
        } else if (nome.toLowerCase().endsWith(".csv")) {
            try (InputStream in = file.getInputStream()) {
                esiti.addAll(importaCsv(nome, in.readAllBytes()));
            }
        } else {
            throw new BadRequestException("Il file deve essere un .csv o uno .zip di csv");
        }
        if (esiti.isEmpty()) throw new BadRequestException("Nessun csv di fattura trovato in " + nome);
        return esiti;
    }

    private List<EsitoImportFatturaFornitoreDTO> importaCsv(String nomeFile, byte[] contenuto) {
        String testo = new String(contenuto, StandardCharsets.UTF_8);
        if (testo.startsWith("﻿")) testo = testo.substring(1);
        String[] linee = testo.split("\r?\n");
        if (linee.length < 2) throw new BadRequestException("Il file " + nomeFile + " non contiene righe");

        Map<String, Integer> colonne = new HashMap<>();
        String[] intestazione = linee[0].split(";", -1);
        for (int i = 0; i < intestazione.length; i++) colonne.put(intestazione[i].trim(), i);
        for (String obbligatoria : List.of("Nr Bolletta", "Nr Punto Di Riconsegna", "Totale Imponibile", "Inizio Periodo")) {
            if (!colonne.containsKey(obbligatoria)) {
                throw new BadRequestException("Il file " + nomeFile + " non ha la colonna '" + obbligatoria + "'");
            }
        }

        // righe per bolletta (di solito una sola per file)
        Map<String, List<VoceFatturaFornitore>> perBolletta = new LinkedHashMap<>();
        Map<String, LocalDate[]> date = new HashMap<>();
        Map<String, Integer> scartate = new HashMap<>();
        for (int n = 1; n < linee.length; n++) {
            if (linee[n].isBlank()) continue;
            Riga r = new Riga(linee[n].split(";", -1), colonne);
            String numero = r.testo("Nr Bolletta");
            try {
                if (numero == null) throw new IllegalArgumentException("numero bolletta mancante");
                VoceFatturaFornitore voce = leggiVoce(r, n);
                perBolletta.computeIfAbsent(numero, k -> new ArrayList<>()).add(voce);
                date.putIfAbsent(numero, new LocalDate[]{r.data("Data Emissione"), r.data("Data Scadenza")});
            } catch (Exception e) {
                log.warn("{}: riga {} scartata: {}", nomeFile, n + 1, e.getMessage());
                scartate.merge(numero != null ? numero : "", 1, Integer::sum);
            }
        }

        Set<String> podAnagrafica = new HashSet<>(fornituraRepository.getListaPod());
        MappaturaVoceFornitoreService.Mappatore mappatore = mappaturaService.mappatore();
        List<EsitoImportFatturaFornitoreDTO> esiti = new ArrayList<>();
        for (Map.Entry<String, List<VoceFatturaFornitore>> e : perBolletta.entrySet()) {
            String numero = e.getKey();
            List<VoceFatturaFornitore> voci = e.getValue();
            Optional<FatturaFornitore> esistente = fatturaFornitoreRepository.findByNumero(numero);
            FatturaFornitore fattura = esistente.orElseGet(FatturaFornitore::new);
            esistente.ifPresent(voceFatturaFornitoreRepository::deleteByFatturaFornitore);

            double totale = voci.stream().mapToDouble(VoceFatturaFornitore::getImporto).sum();
            fattura.setNumero(numero);
            fattura.setDataEmissione(date.get(numero)[0]);
            fattura.setDataScadenza(date.get(numero)[1]);
            fattura.setNomeFile(nomeFile);
            fattura.setDataImport(LocalDateTime.now());
            fattura.setRighe(voci.size());
            fattura.setTotaleImponibile(arrotonda(totale));
            fattura = fatturaFornitoreRepository.save(fattura);
            for (VoceFatturaFornitore voce : voci) voce.setFatturaFornitore(fattura);
            voceFatturaFornitoreRepository.saveAll(voci);

            List<String> podSconosciuti = voci.stream().map(VoceFatturaFornitore::getPod)
                    .filter(p -> p != null && !podAnagrafica.contains(p)).distinct().sorted().toList();
            int nonMappate = (int) voci.stream().filter(v -> mappatore.regola(v) == null).count();
            log.info("Fattura fornitore {} ({}): {} righe, imponibile {}, {} POD sconosciuti, {} righe non mappate",
                    numero, nomeFile, voci.size(), arrotonda(totale), podSconosciuti.size(), nonMappate);
            esiti.add(new EsitoImportFatturaFornitoreDTO(numero, fattura.getDataEmissione(), nomeFile,
                    esistente.isPresent(), voci.size(), scartate.getOrDefault(numero, 0), arrotonda(totale),
                    podSconosciuti, nonMappate));
        }
        return esiti;
    }

    private VoceFatturaFornitore leggiVoce(Riga r, int n) {
        VoceFatturaFornitore v = new VoceFatturaFornitore();
        Double importo = r.numero("Totale Imponibile");
        if (importo == null) throw new IllegalArgumentException("totale imponibile mancante");
        v.setRiga(n);
        v.setPod(r.testo("Nr Punto Di Riconsegna"));
        v.setNrUtenza(r.testo("Nr Utenza"));
        v.setClasseUtenza(r.testo("Classe utenza"));
        v.setInizioPeriodo(r.data("Inizio Periodo"));
        v.setFinePeriodo(r.data("Fine Periodo"));
        // competenza: mese di inizio periodo (anche per le righe a cavallo di due mesi)
        LocalDate competenza = v.getInizioPeriodo() != null ? v.getInizioPeriodo()
                : v.getFinePeriodo() != null ? v.getFinePeriodo() : r.data("Data Emissione");
        if (competenza == null) throw new IllegalArgumentException("periodo mancante");
        v.setMese(competenza.getMonthValue());
        v.setAnno(competenza.getYear());
        String consumoReale = r.testo("Consumo Reale");
        v.setConsumoReale(consumoReale == null ? null : consumoReale.equalsIgnoreCase("Si"));
        v.setUnita(r.testo("U M"));
        v.setQuantita(r.numero("Quantità"));
        v.setUnitario(r.numero("Unitario"));
        v.setPercIva(r.numero("Perc IVA"));
        v.setImporto(importo);
        v.setDescrizione(r.testo("Descrizione Tariffa"));
        v.setCodOfferta(r.testo("Cod Offerta"));
        v.setCodTariffa(r.testo("Cod Tariffa"));
        v.setNrArticolo(r.testo("Nr Articolo"));
        v.setTipoRicalcolo(r.testo("Tipo Ricalcolo"));
        v.setMotivoRicalcolo(r.testo("Motivo Ricalcolo"));
        v.setTipoMercato(r.testo("Tipo Mercato"));
        v.setPotenzaImpegnata(r.numero("Potenza Impegnata kW"));
        v.setPotenzaDisponibile(r.numero("Potenza Disponibile kW"));
        v.setUnitarioOriginale(r.numero("Unitario Originale"));
        v.setUnitarioAggiuntivo(r.numero("Unitario Aggiuntivo"));
        v.setSpread(r.numero("Spread"));
        return v;
    }

    private static double arrotonda(double valore) {
        return Math.round(valore * 100) / 100.0;
    }

    /**
     * Valori di una riga del csv per nome di colonna (colonne assenti o celle vuote → null).
     */
    private record Riga(String[] celle, Map<String, Integer> colonne) {
        String testo(String colonna) {
            Integer i = colonne.get(colonna);
            if (i == null || i >= celle.length) return null;
            String valore = celle[i].trim();
            return valore.isEmpty() ? null : valore;
        }

        Double numero(String colonna) {
            String valore = testo(colonna);
            return valore == null ? null : Double.parseDouble(valore.replaceAll(",", "."));
        }

        LocalDate data(String colonna) {
            String valore = testo(colonna);
            return valore == null ? null : LocalDate.parse(valore, DATA);
        }
    }
}
