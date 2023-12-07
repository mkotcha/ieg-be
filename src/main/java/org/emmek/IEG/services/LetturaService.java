package org.emmek.IEG.services;

import jakarta.xml.bind.JAXBException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.emmek.IEG.configs.JaxbParser;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
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
import java.io.IOException;
import java.util.UUID;

@Service
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
        File[] listOfZipFiles = folder.listFiles();

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
                            if (datiPod.misura.ea.get(0).valore.equals("31")) {

                                System.out.println(datiPod.pod + " " + fornitura.getCliente().getRagioneSociale());
                                System.out.println("misura del " + datiPod.misura.ea.get(0).valore);
                            }
                        } catch (Exception ignored) {
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
}
