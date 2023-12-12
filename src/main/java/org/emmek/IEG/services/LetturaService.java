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
import org.emmek.IEG.helpers.xml.DatiPod;
import org.emmek.IEG.helpers.xml.FlussoMisure;
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

    public Page<Lettura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return letturaRepository.findAll(pageable);
    }

    public String uploadFlussi(MultipartFile body) throws IOException, JAXBException {
        String destination = "data/uploads";
        deleteAllFilesInFolder(destination);
        File zip = File.createTempFile(UUID.randomUUID().toString(), "temp");
        System.out.println("zip: " + zip.getAbsolutePath());
        FileOutputStream o = new FileOutputStream(zip);
        IOUtils.copy(body.getInputStream(), o);
        o.close();

        ZipFile zipFile = new ZipFile(zip);
        zipFile.extractAll(destination);
        System.out.println("estratti ");

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
            System.out.println("eliminati i file zip");
        }

        File[] listOfXmlFiles = folder.listFiles();

        if (listOfXmlFiles != null) {
            System.out.println("trovati " + listOfXmlFiles.length + " file XML");
            for (File file : listOfXmlFiles) {
                if (file.isFile()) {
                    FlussoMisure flussi = jaxbParser.parseXml(file.getAbsolutePath());
                    for (DatiPod datiPod : flussi.datiPod) {
                        try {
                            Fornitura fornitura = fornituraService.finById(datiPod.pod);
                            if (datiPod.misura.validato.equals("S") && datiPod.misura.ea.get(0).valore.equals("30")) {
                                Lettura lettura = new Lettura();
                                lettura.setFornitura(fornitura);
                                String meseAnno = datiPod.meseAnno;
                                int mese = Integer.parseInt(meseAnno.substring(0, 2));
                                int anno = Integer.parseInt(meseAnno.substring(2, 6));
                                int giorno = Integer.parseInt(datiPod.misura.ea.get(0).valore);
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
                                lettura.setPotMax(datiPod.misura.potMax);
                                lettura.setEaF1(Double.parseDouble(datiPod.misura.eaF1));
                                lettura.setEaF2(Double.parseDouble(datiPod.misura.eaF2));
                                lettura.setEaF3(Double.parseDouble(datiPod.misura.eaF3));
                                lettura.setErF1(Double.parseDouble(datiPod.misura.erF1));
                                lettura.setErF2(Double.parseDouble(datiPod.misura.erF2));
                                lettura.setErF3(Double.parseDouble(datiPod.misura.erF3));
                                lettura.setPotF1(Double.parseDouble(datiPod.misura.potF1));
                                lettura.setPotF2(Double.parseDouble(datiPod.misura.potF2));
                                lettura.setPotF3(Double.parseDouble(datiPod.misura.potF3));

                                System.out.println(file.getName());
                                System.out.println(datiPod.pod + " " + fornitura.getCliente().getRagioneSociale());
                                System.out.println("misura del " + datiPod.misura.ea.get(0).valore);

                                letturaRepository.save(lettura);
                            }
                        } catch (Exception e) {
                            log.debug("lettura non importata da file xml - " + e.getMessage());
                        }
                    }
                }
            }
        }

        System.out.println("finito");
        return "fino a qui tutto bene...";
    }

    private void deleteAllFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty directories
            for (File f : files) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }
    }

    public void save(Lettura lettura) {
        letturaRepository.save(lettura);
    }
}
