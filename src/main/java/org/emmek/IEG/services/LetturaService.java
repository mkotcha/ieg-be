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
import org.emmek.IEG.helpers.xml.DatiPod;
import org.emmek.IEG.helpers.xml.FlussoMisure;
import org.emmek.IEG.payloads.LetturaDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LetturaService {

    @Autowired
    private LetturaRepository letturaRepository;

    @Autowired
    private FornituraService fornituraService;

    @Autowired
//    private Function<String, FlussoMisure> parseXmlFunction;
    private JaxbParser jaxbParser;

    public Page<Lettura> findAll(int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return letturaRepository.findAllFetched(pageable);
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
                    log.info("sovrascrivo lettura per il pod {} del {}/{}/{} - {}", datiPod.pod, giorno, mese, anno, fornitura.getCliente().getRagioneSociale());
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

    public long getNextId() {
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
        double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        double potMax = Math.max(potF1, Math.max(potF2, potF3));
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

    public Lettura get(long id) {
        return letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));

    }

    public Lettura update(long id, LetturaDTO body) {
        Lettura lettura = letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));
        lettura.setDataLettura(LocalDate.parse(body.dataLettura()));
        lettura.setTipoContatore(TipoContatore.valueOf(body.tipoContatore()));
        lettura.setUtile(true);
        lettura.setTipoLettura(TipoLettura.valueOf(body.tipoLettura()));
        lettura.setRaccolta(body.raccolta());
        lettura.setTipoDato(body.tipoDato());
        lettura.setValidato(body.validato());
        double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        double potMax = Math.max(potF1, Math.max(potF2, potF3));
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
        if (letture.size() > 2) throw new RuntimeException("Troppe Letture per calcolare il consumo");
        Lettura letturaLast = letture.get(0);
        Lettura letturaOld = letture.get(1);
        consumi.put("EaF1", letturaLast.getEaF1() - letturaOld.getEaF1());
        consumi.put("EaF2", letturaLast.getEaF2() - letturaOld.getEaF2());
        consumi.put("EaF3", letturaLast.getEaF3() - letturaOld.getEaF3());
        consumi.put("ErF1", letturaLast.getErF1() - letturaOld.getErF1());
        consumi.put("ErF2", letturaLast.getErF2() - letturaOld.getErF2());
        consumi.put("ErF3", letturaLast.getErF3() - letturaOld.getErF3());
        consumi.put("perditeF1", consumi.get("EaF1") / 100 * 10.4);
        consumi.put("perditeF2", consumi.get("EaF2") / 100 * 10.4);
        consumi.put("perditeF3", consumi.get("EaF3") / 100 * 10.4);
        double consumoTot = consumi.get("EaF1") + consumi.get("EaF2") + consumi.get("EaF3");
        double consumoTotP = consumoTot + consumi.get("perditeF1") + consumi.get("perditeF2") + consumi.get("perditeF3");
        double consumoTotR = consumi.get("ErF1") + consumi.get("ErF2") + consumi.get("ErF3");
        double potMax = Math.max(letturaLast.getPotF1(), Math.max(letturaLast.getPotF2(), letturaLast.getPotF3()));
        consumi.put("consumoTot", consumoTot);
        consumi.put("consumoTotP", consumoTotP);
        consumi.put("consumoTotR", consumoTotR);
        consumi.put("potMax", potMax);
        if (consumoTotR > 0) {
            double percentualeReattiva = (consumi.get("consumoTotR") / consumi.get("consumoTot")) * 100;
            consumi.put("percentualeReattiva", percentualeReattiva);
        }
        return consumi;
    }

    public Map<String, Double> getConsumi(Fornitura fornitura, int mese) {
        mese = mese + 1;
        LocalDate date = LocalDate.now().minusMonths(mese);
        List<Lettura> letture = getLetture(fornitura, date.getMonthValue(), date.getYear());
        return getConsumi(letture);
    }

}
