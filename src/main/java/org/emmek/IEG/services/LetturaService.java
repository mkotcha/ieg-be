package org.emmek.IEG.services;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
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
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

@Service
public class LetturaService {

    @Autowired
    private LetturaRepository letturaRepository;

    @Autowired
    private FornituraService fornituraService;

    public Page<Lettura> findAll(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return letturaRepository.findAll(pageable);
    }

    public String uploadFlussi(MultipartFile body) throws IOException, JAXBException {
        String source = body.getOriginalFilename();
        String destination = "data/uploads";

        File zip = File.createTempFile(UUID.randomUUID().toString(), "temp");
        FileOutputStream o = new FileOutputStream(zip);
        IOUtils.copy(body.getInputStream(), o);
        o.close();

        ZipFile zipFile = new ZipFile(zip);
        zipFile.extractAll(destination);

        File folder = new File(destination);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    try (ZipFile innerZipFile = new ZipFile(file)) {
                        innerZipFile.extractAll(destination);
                    }
                    boolean deleted = file.delete();

                }
            }
        }
        JAXBContext context = JAXBContext.newInstance(FlussoMisure.class);

        listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    FlussoMisure flussi = (FlussoMisure) context.createUnmarshaller()
                            .unmarshal(new FileReader(file.getAbsolutePath()));

                    for (DatiPod datiPod : flussi.getDatiPod()) {

                        try {
                            Fornitura fornitura = fornituraService.finById(datiPod.getPod());
                            System.out.println(datiPod.getPod());
                            System.out.println("trovato " + datiPod.getMisura());
                        } catch (Exception ignored) {

                        }

                    }


                }
            }
        }


        return "fino a qui tutto bene...";

    }
}
