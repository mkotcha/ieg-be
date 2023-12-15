package org.emmek.IEG.services;

import jakarta.xml.bind.JAXBException;
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
import org.emmek.IEG.payloads.LetturaPostDTO;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.LocalDate;
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
        return letturaRepository.findAll(pageable);
    }

    public String uploadFlussi(MultipartFile body) throws IOException, JAXBException {
        String destination = "data/uploads";
        deleteAllFilesInFolder(destination);
        File zip = File.createTempFile(UUID.randomUUID().toString(), "_tmp.zip");
        log.debug("zip: " + zip.getAbsolutePath());
        FileOutputStream o = new FileOutputStream(zip);
        IOUtils.copy(body.getInputStream(), o);
        o.close();

        ZipFile zipFile = new ZipFile(zip);
        zipFile.extractAll(destination);
        log.debug("estrazione completata");

        File folder = new File(destination);
        File[] listOfZipFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".zip");
            }
        });

        if (listOfZipFiles != null) {
            for (File file : listOfZipFiles) {
                if (file.isFile()) {
                    try (ZipFile innerZipFile = new ZipFile(file)) {
                        innerZipFile.extractAll(destination);
                    }
                    boolean deleted = file.delete();
                }
            }
            log.debug("eliminati i file zip");
        }

        File[] listOfFiles = folder.listFiles();

        File[] listOfXmlFiles = folder.listFiles((dir, name) -> name.contains("_PDO") || name.contains("_PNO"));
        if (listOfXmlFiles != null && listOfFiles != null) {
            log.debug("esamino " + listOfXmlFiles.length + " file XML su " + listOfFiles.length);
            for (File file : listOfXmlFiles) {
                FlussoMisure flussi = jaxbParser.parseXml(file.getAbsolutePath());
                log.debug("file: " + file.getName());
                for (DatiPod datiPod : flussi.datiPod) {

                    parseDatiPod(datiPod);

                }
            }
        }

        log.debug("finito");
        return "fino a qui tutto bene...";
    }

    public void parseDatiPod(DatiPod datiPod) {
        Lettura lettura = new Lettura();
        Fornitura fornitura = null;
        try {
            fornitura = fornituraService.finById(datiPod.pod);
        } catch (Exception ignore) {
        }
        int giorno;
        if (datiPod.misura.ea != null) {
            giorno = Integer.parseInt(datiPod.misura.ea.get(0).valore);
        } else {
            giorno = Integer.parseInt(datiPod.dataMisura.substring(0, 2));
        }
        if (fornitura != null
                && datiPod.misura.validato.equals("S")
                && giorno == 30) {
            try {
                log.debug("parsing...");
                lettura.setId(getNextId());
                lettura.setFornitura(fornitura);
                int anno;
                int mese;
                if (datiPod.meseAnno != null) {
                    String meseAnno = datiPod.meseAnno;
                    mese = Integer.parseInt(meseAnno.substring(0, 2));
                    anno = Integer.parseInt(meseAnno.substring(3, 7));
                } else {
                    String dataMisura = datiPod.dataMisura;
                    mese = Integer.parseInt(dataMisura.substring(3, 5));
                    anno = Integer.parseInt(dataMisura.substring(6, 10));
                }
                lettura.setDataLettura(LocalDate.of(anno, mese, giorno));
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
                lettura.setPotMax(datiPod.misura.potMax.replaceAll(",", "."));
                lettura.setEaF1(Double.parseDouble(datiPod.misura.eaF1.replaceAll(",", ".")));
                lettura.setEaF2(Double.parseDouble(datiPod.misura.eaF2.replaceAll(",", ".")));
                lettura.setEaF3(Double.parseDouble(datiPod.misura.eaF3.replaceAll(",", ".")));
                lettura.setErF1(Double.parseDouble(datiPod.misura.erF1.replaceAll(",", ".")));
                lettura.setErF2(Double.parseDouble(datiPod.misura.erF2.replaceAll(",", ".")));
                lettura.setErF3(Double.parseDouble(datiPod.misura.erF3.replaceAll(",", ".")));
                lettura.setPotF1(Double.parseDouble(datiPod.misura.potF1.replaceAll(",", ".")));
                lettura.setPotF2(Double.parseDouble(datiPod.misura.potF2.replaceAll(",", ".")));
                lettura.setPotF3(Double.parseDouble(datiPod.misura.potF3.replaceAll(",", ".")));

                log.debug(datiPod.pod + " " + fornitura.getCliente().getRagioneSociale());
                log.debug("lettura del " + giorno + "/" + mese + "/" + anno);

                letturaRepository.save(lettura);
            } catch (Exception e) {
                log.error("lettura non importata da file xml - " + e.getMessage());
            }
        }
    }

    private void deleteAllFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty directories
            for (File f : files) {
                if (f.isFile()) {
                    boolean isDeleted = f.delete();
                    if (!isDeleted) {
                        log.error("Failed to delete file: " + f.getName());
                    }
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

    public Lettura save(LetturaPostDTO body) {
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

    public Lettura update(long id, LetturaPostDTO body) {
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
        lettura.setNote(body.note());

        return letturaRepository.save(lettura);
    }
}
