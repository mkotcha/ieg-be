package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.emmek.IEG.configs.JaxbParser;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.enums.TipoContatore;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.model.xml.DatiPod;
import org.emmek.IEG.model.xml.FlussoMisure;
import org.emmek.IEG.payloads.LetturaDTO;
import org.emmek.IEG.repositories.FatturaSingolaRepository;
import org.emmek.IEG.repositories.LetturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LetturaService {

    // perdite di rete in percentuale sul consumo
    private static final double PERCENTUALE_PERDITE = 10;

    @Autowired
    private LetturaRepository letturaRepository;

    @Autowired
    private FatturaSingolaRepository fatturaSingolaRepository;

    @Autowired
    private FornituraService fornituraService;

    @Autowired
//    private Function<String, FlussoMisure> parseXmlFunction;
    private JaxbParser jaxbParser;

    public Page<Lettura> findAll(int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return letturaRepository.findAll(pageable);
    }

    public String uploadFlussi(MultipartFile body) throws IOException {
        String destination = "data/uploads";
        deleteAllFilesInFolder(destination);
        File zip = File.createTempFile(UUID.randomUUID().toString(), "_tmp.zip");
        log.debug("zip: {}", zip.getAbsolutePath());
        try (FileOutputStream o = new FileOutputStream(zip)) {
            IOUtils.copy(body.getInputStream(), o);
        }

        try (ZipFile zipFile = new ZipFile(zip)) { // try-with-resources
            zipFile.extractAll(destination);
        } catch (Exception e) {
            log.error("Errore durante l'estrazione del file ZIP: {}", zip.getName(), e);
        }

        log.debug("estrazione completata");

        processZipFilesRecursively(new File(destination), destination);
        processXmlFilesRecursively(new File(destination));

        log.debug("finito");
        return "fino a qui tutto bene...";
    }

    private void processXmlFilesRecursively(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processXmlFilesRecursively(file);
                } else if (file.getName().toLowerCase().endsWith(".xml") &&
                        (file.getName().contains("_PDO") || file.getName().contains("_PNO") || file.getName().contains("_SNM2G"))) {
                    log.debug("Processing XML file: {}", file.getAbsolutePath());
                    try {
                        FlussoMisure flussi = jaxbParser.parseXml(file.getAbsolutePath());
                        for (DatiPod datiPod : flussi.datiPod) {
                            parseDatiPod(datiPod);
                        }
                    } catch (Exception e) {
                        log.error("Errore durante il parsing del file XML: {}", file.getName());
                    }
                }
            }
        }
    }

    private void processZipFilesRecursively(File folder, String destination) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processZipFilesRecursively(file, destination);
                } else if (file.getName().toLowerCase().endsWith(".zip")) {
                    try (ZipFile innerZipFile = new ZipFile(file)) {
                        innerZipFile.extractAll(destination);
                    }
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.error("Impossibile eliminare il file ZIP: {}", file.getName());
                    }
                }
            }
        }
    }

    public void parseDatiPod(DatiPod datiPod) {
        Lettura lettura;
        Fornitura fornitura = null;
        try {
            fornitura = fornituraService.finById(datiPod.pod);
        } catch (Exception ignore) {
        }
        if (fornitura != null) {
            int giorno;
            int mese;
            int anno;

            if (datiPod.misura.ea != null) {
                giorno = Integer.parseInt(datiPod.misura.ea.get(0).valore);
            } else {
                if (datiPod.dataMisura != null) {
                    giorno = Integer.parseInt(datiPod.dataMisura.substring(0, 2));
                } else {
                    giorno = Integer.parseInt(datiPod.dataPrest.substring(0, 2));
                }
            }
            if (datiPod.meseAnno != null) {
                String meseAnno = datiPod.meseAnno;
                mese = Integer.parseInt(meseAnno.substring(0, 2));
                anno = Integer.parseInt(meseAnno.substring(3, 7));
            } else {
                String dataMisura;
                if (datiPod.dataMisura != null) {
                    dataMisura = datiPod.dataMisura;
                } else {
                    dataMisura = datiPod.dataPrest;
                }
                mese = Integer.parseInt(dataMisura.substring(3, 5));
                anno = Integer.parseInt(dataMisura.substring(6, 10));
            }

            //  && datiPod.misura.validato.equals("S")
//            if (giorno == 30 && mese == 9 && anno == 2025) {
//            if (true) {
            try {
//                    log.info("parsing... {} {}", datiPod.pod, fornitura.getCliente().getRagioneSociale());
                LocalDate dataLettura = LocalDate.of(anno, mese, giorno);
                List<Lettura> lettureEsistenti = letturaRepository.findByFornituraAndDataLettura(fornitura, dataLettura);
                lettura = lettureEsistenti.isEmpty() ? null : lettureEsistenti.get(0);
                if (lettura == null) {
                    lettura = new Lettura();
                    lettura.setId(getNextId());
                    lettura.setFornitura(fornitura);
                    lettura.setDataLettura(LocalDate.of(anno, mese, giorno));
                    log.info("inserisco -------> lettura pod {} del {}/{}/{} - {}", datiPod.pod, giorno, mese, anno, fornitura.getCliente().getRagioneSociale());
                } else {
//                    log.info("sovrascrivo lettura per il pod {} del {}/{}/{} - {}", datiPod.pod, giorno, mese, anno, fornitura.getCliente().getRagioneSociale());
                }

                switch (datiPod.datiPdp.trattamento) {
                    case "O" -> lettura.setTipoContatore(TipoContatore.ORARIO);
                    case "F" -> lettura.setTipoContatore(TipoContatore.FASCIA);
                    case "M" -> lettura.setTipoContatore(TipoContatore.MONORARIO);
                }
                lettura.setUtile(true);
                switch (datiPod.misura.tipoDato) {
                    case "E" -> lettura.setTipoLettura(TipoLettura.REALE);
                    case "S" -> lettura.setTipoLettura(TipoLettura.STIMA);
                }
                lettura.setRaccolta(datiPod.misura.raccolta);
                lettura.setTipoDato(datiPod.misura.tipoDato);
                lettura.setCausaOstativa(datiPod.misura.causaOstativa);
                lettura.setValidato(datiPod.misura.validato);
                if (datiPod.misura.potMax != null) {
                    lettura.setPotMax(datiPod.misura.potMax.replaceAll(",", "."));
                }
                lettura.setEaF1(Double.parseDouble(datiPod.misura.eaF1.replaceAll(",", ".")));
                lettura.setEaF2(Double.parseDouble(datiPod.misura.eaF2.replaceAll(",", ".")));
                lettura.setEaF3(Double.parseDouble(datiPod.misura.eaF3.replaceAll(",", ".")));
                lettura.setErF1(Double.parseDouble(datiPod.misura.erF1.replaceAll(",", ".")));
                lettura.setErF2(Double.parseDouble(datiPod.misura.erF2.replaceAll(",", ".")));
                lettura.setErF3(Double.parseDouble(datiPod.misura.erF3.replaceAll(",", ".")));
                lettura.setErcF1(parseDecimale(datiPod.misura.ercF1));
                lettura.setErcF2(parseDecimale(datiPod.misura.ercF2));
                lettura.setErcF3(parseDecimale(datiPod.misura.ercF3));
                lettura.setPotF1(Double.parseDouble(datiPod.misura.potF1.replaceAll(",", ".")));
                lettura.setPotF2(Double.parseDouble(datiPod.misura.potF2.replaceAll(",", ".")));
                lettura.setPotF3(Double.parseDouble(datiPod.misura.potF3.replaceAll(",", ".")));
                lettura.setKa(Double.parseDouble(datiPod.datiPdp.ka.replaceAll(",", ".")));
                lettura.setKr(Double.parseDouble(datiPod.datiPdp.kr.replaceAll(",", ".")));
                lettura.setKp(Double.parseDouble(datiPod.datiPdp.kp.replaceAll(",", ".")));

//                    log.debug(datiPod.pod + " " + fornitura.getCliente().getRagioneSociale());
//                    log.info("lettura del {}/{}/{}", giorno, mese, anno);

                letturaRepository.save(lettura);
            } catch (Exception e) {
                log.error("lettura non importata da file xml - {}", e.getMessage());
            }
//            }
        }
    }

    private void deleteAllFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteAllFilesInFolder(f.getAbsolutePath()); // Elimina i contenuti della directory
                }
                boolean isDeleted = f.delete(); // Elimina il file o la directory vuota
                if (!isDeleted) {
                    log.error("Failed to delete: {}", f.getName());
                }
            }
        }
    }

    public Long getNextId() {
        Long maxId = letturaRepository.findMaxId();
        return (maxId == null ? 1 : maxId + 1);
    }

    public void save(Lettura lettura) {
        letturaRepository.save(lettura);
    }

    public Lettura save(LetturaDTO body) {
        Lettura lettura = new Lettura();
        Fornitura fornitura = null;
        try {
            fornitura = fornituraService.finById(body.pod());
        } catch (Exception ignore) {
        }
        lettura.setId(getNextId());
        lettura.setFornitura(fornitura);
        lettura.setDataLettura(LocalDate.parse(body.dataLettura()));
        lettura.setTipoContatore(TipoContatore.valueOf(body.tipoContatore()));
        lettura.setUtile(true);
        lettura.setTipoLettura(TipoLettura.valueOf(body.tipoLettura()));
        lettura.setRaccolta(body.raccolta());
        lettura.setTipoDato(body.tipoDato());
        lettura.setValidato(body.validato());
        Double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        Double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        Double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        Double potMax = Math.max(potF1, Math.max(potF2, potF3));
        lettura.setPotMax(String.valueOf(potMax));
        lettura.setEaF1(Double.parseDouble(body.eaF1().replaceAll(",", ".")));
        lettura.setEaF2(Double.parseDouble(body.eaF2().replaceAll(",", ".")));
        lettura.setEaF3(Double.parseDouble(body.eaF3().replaceAll(",", ".")));
        lettura.setErF1(Double.parseDouble(body.erF1().replaceAll(",", ".")));
        lettura.setErF2(Double.parseDouble(body.erF2().replaceAll(",", ".")));
        lettura.setErF3(Double.parseDouble(body.erF3().replaceAll(",", ".")));
        lettura.setPotF1(potF1);
        lettura.setPotF2(potF2);
        lettura.setPotF3(potF3);
        lettura.setKa(Double.parseDouble(body.ka().replaceAll(",", ".")));
        lettura.setKr(Double.parseDouble(body.kr().replaceAll(",", ".")));
        lettura.setKp(Double.parseDouble(body.kp().replaceAll(",", ".")));
        lettura.setNote(body.note());

        return letturaRepository.save(lettura);
    }

    public void delete(Long id) {
        Lettura lettura = letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));
        letturaRepository.delete(lettura);
    }

    public Lettura get(Long id) {
        return letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));

    }

    public Lettura update(Long id, LetturaDTO body) {
        Lettura lettura = letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));
        lettura.setDataLettura(LocalDate.parse(body.dataLettura()));
        lettura.setTipoContatore(TipoContatore.valueOf(body.tipoContatore()));
        lettura.setUtile(true);
        lettura.setTipoLettura(TipoLettura.valueOf(body.tipoLettura()));
        lettura.setRaccolta(body.raccolta());
        lettura.setTipoDato(body.tipoDato());
        lettura.setValidato(body.validato());
        Double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        Double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        Double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        Double potMax = Math.max(potF1, Math.max(potF2, potF3));
        lettura.setPotMax(String.valueOf(potMax));
        lettura.setEaF1(Double.parseDouble(body.eaF1().replaceAll(",", ".")));
        lettura.setEaF2(Double.parseDouble(body.eaF2().replaceAll(",", ".")));
        lettura.setEaF3(Double.parseDouble(body.eaF3().replaceAll(",", ".")));
        lettura.setErF1(Double.parseDouble(body.erF1().replaceAll(",", ".")));
        lettura.setErF2(Double.parseDouble(body.erF2().replaceAll(",", ".")));
        lettura.setErF3(Double.parseDouble(body.erF3().replaceAll(",", ".")));
        lettura.setPotF1(potF1);
        lettura.setPotF2(potF2);
        lettura.setPotF3(potF3);
        lettura.setKa(Double.parseDouble(body.ka().replaceAll(",", ".")));
        lettura.setKr(Double.parseDouble(body.kr().replaceAll(",", ".")));
        lettura.setKp(Double.parseDouble(body.kp().replaceAll(",", ".")));
        lettura.setNote(body.note());
        return letturaRepository.save(lettura);
    }

    public List<Lettura> getLetture(Fornitura fornitura, int mese, int anno) {
        LocalDate from;
        LocalDate to;
        LocalDate primoDelMese = LocalDate.of(anno, mese, 1);
        LocalDate mesePrecedente = primoDelMese.minusMonths(1);
        // inizio periodo: lettura finale della fattura del mese precedente; se non c'è, la lettura di switch per
        // i clienti nuovi (switch nel mese), altrimenti la lettura di fine mese precedente
        from = fatturaSingolaRepository
                .findDataUltimaLetturaFatturata(fornitura, mesePrecedente.getMonthValue(), mesePrecedente.getYear())
                .orElseGet(() -> {
                    LocalDate dataSwitch = fornitura.getDataSwitch();
                    if (dataSwitch != null && !dataSwitch.isBefore(primoDelMese)) return dataSwitch;
                    return primoDelMese.minusDays(1);
                });
        to = primoDelMese.withDayOfMonth(primoDelMese.lengthOfMonth());
        List<Lettura> letture = letturaRepository.findByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(fornitura, from, to);
        if (letture.size() < 2)
            throw new RuntimeException(fornitura.getId() + " non ci sono abbastanza letture per calcolare il consumo per il mese " + mese + " dell'anno " + anno);
        return letture;
    }

    public int contaLetture(Fornitura fornitura, int mese, int anno) {
        LocalDate from;
        LocalDate to;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (mese == 1) {
            from = LocalDate.parse((anno - 1) + "-12-15", formatter);
        } else {
            from = LocalDate.parse(anno + "-" + String.format("%02d", mese - 1) + "-15", formatter);
        }
        if (mese == 12) {
            to = LocalDate.parse((anno + 1) + "-01-15", formatter);
        } else {
            to = LocalDate.parse(anno + "-" + String.format("%02d", mese + 1) + "-15", formatter);
        }
        return letturaRepository.countByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(fornitura, from, to);
    }

    public Map<String, Double> getConsumi(List<Lettura> letture) {
        Map<String, Double> consumi = new HashMap<>();
        if (letture.size() < 2) throw new RuntimeException("Non ci sono abbastanza letture per calcolare il consumo");
        List<Lettura> ordinate = letture.stream()
                .sorted(Comparator.comparing(Lettura::getDataLettura))
                .toList();
        double eaF1 = 0, eaF2 = 0, eaF3 = 0, erF1 = 0, erF2 = 0, erF3 = 0, ercF1 = 0, ercF2 = 0, ercF3 = 0;
        double potMax = 0;
        // somma dei consumi tra letture consecutive (senza cambi contatore equivale a ultima - prima).
        // Le letture vanno moltiplicate per le costanti del contatore: ka attiva, kr reattiva, kp potenza.
        // TODO: gestire le letture di CAMBIO contatore (sommare i consumi fino al cambio e ripartire da zero)
        for (int i = 1; i < ordinate.size(); i++) {
            Lettura prec = ordinate.get(i - 1);
            Lettura curr = ordinate.get(i);
            eaF1 += nvl(curr.getEaF1()) * ka(curr) - nvl(prec.getEaF1()) * ka(prec);
            eaF2 += nvl(curr.getEaF2()) * ka(curr) - nvl(prec.getEaF2()) * ka(prec);
            eaF3 += nvl(curr.getEaF3()) * ka(curr) - nvl(prec.getEaF3()) * ka(prec);
            erF1 += nvl(curr.getErF1()) * kr(curr) - nvl(prec.getErF1()) * kr(prec);
            erF2 += nvl(curr.getErF2()) * kr(curr) - nvl(prec.getErF2()) * kr(prec);
            erF3 += nvl(curr.getErF3()) * kr(curr) - nvl(prec.getErF3()) * kr(prec);
            // reattiva immessa: solo tra letture che hanno il dato (le letture importate prima non lo hanno)
            ercF1 += delta(curr.getErcF1(), prec.getErcF1(), kr(curr), kr(prec));
            ercF2 += delta(curr.getErcF2(), prec.getErcF2(), kr(curr), kr(prec));
            ercF3 += delta(curr.getErcF3(), prec.getErcF3(), kr(curr), kr(prec));
            potMax = Math.max(potMax, kp(curr) * Math.max(nvl(curr.getPotF1()), Math.max(nvl(curr.getPotF2()), nvl(curr.getPotF3()))));
        }
        consumi.put("EaF1", eaF1);
        consumi.put("EaF2", eaF2);
        consumi.put("EaF3", eaF3);
        consumi.put("ErF1", erF1);
        consumi.put("ErF2", erF2);
        consumi.put("ErF3", erF3);
        consumi.put("ErcF1", ercF1);
        consumi.put("ErcF2", ercF2);
        consumi.put("ErcF3", ercF3);
        consumi.put("perditeF1", consumi.get("EaF1") * PERCENTUALE_PERDITE / 100);
        consumi.put("perditeF2", consumi.get("EaF2") * PERCENTUALE_PERDITE / 100);
        consumi.put("perditeF3", consumi.get("EaF3") * PERCENTUALE_PERDITE / 100);
        Double consumoTot = consumi.get("EaF1") + consumi.get("EaF2") + consumi.get("EaF3");
        Double consumoTotP = consumoTot + consumi.get("perditeF1") + consumi.get("perditeF2") + consumi.get("perditeF3");
        Double consumoTotR = consumi.get("ErF1") + consumi.get("ErF2") + consumi.get("ErF3");
        consumi.put("consumoTot", consumoTot);
        consumi.put("consumoTotP", consumoTotP);
        consumi.put("consumoTotR", consumoTotR);
        consumi.put("potMax", potMax);
        if (consumoTotR > 0) {
            Double percentualeReattiva = (consumi.get("consumoTotR") / consumi.get("consumoTot")) * 100;
            consumi.put("percentualeReattiva", percentualeReattiva);
        }
        return consumi;
    }

    /**
     * Consumo totale di energia attiva calcolato solo tra la prima e l'ultima lettura,
     * senza considerare le letture intermedie: serve per verificare il risultato di getConsumi.
     */
    public Double getConsumoTotPrimaUltima(Lettura prima, Lettura ultima) {
        return (nvl(ultima.getEaF1()) * ka(ultima) - nvl(prima.getEaF1()) * ka(prima))
                + (nvl(ultima.getEaF2()) * ka(ultima) - nvl(prima.getEaF2()) * ka(prima))
                + (nvl(ultima.getEaF3()) * ka(ultima) - nvl(prima.getEaF3()) * ka(prima));
    }

    private static double delta(Double corrente, Double precedente, double kCorrente, double kPrecedente) {
        if (corrente == null || precedente == null) return 0;
        return corrente * kCorrente - precedente * kPrecedente;
    }

    private static Double parseDecimale(String valore) {
        return valore != null && !valore.isBlank() ? Double.parseDouble(valore.replaceAll(",", ".")) : null;
    }

    // costanti del contatore: se mancano valgono 1
    private static double ka(Lettura l) {
        return l.getKa() != null ? l.getKa() : 1;
    }

    private static double kr(Lettura l) {
        return l.getKr() != null ? l.getKr() : 1;
    }

    private static double kp(Lettura l) {
        return l.getKp() != null ? l.getKp() : 1;
    }

    private static double nvl(Double valore) {
        return valore != null ? valore : 0;
    }

    public Map<String, Double> getConsumi(Fornitura fornitura, int mese) {
        mese = mese + 1;
        LocalDate date = LocalDate.now().minusMonths(mese);
        List<Lettura> letture = getLetture(fornitura, date.getMonthValue(), date.getYear());
        return getConsumi(letture);
    }

}
