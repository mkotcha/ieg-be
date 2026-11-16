package org.emmek.IEG.services;

import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.payloads.ParteZipDTO;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Pdf delle fatture di un mese divisi in più zip da inviare per email: ogni zip sta sotto la dimensione massima
 * configurata ({@code fatture.zip.max-mb}). Il limite va tenuto sotto quello del messaggio perché l'allegato in
 * base64 cresce di circa un terzo (20 MB a messaggio → zip da 14 MB).
 * La divisione dipende solo dai file su disco, quindi elenco delle parti e download della singola parte coincidono.
 */
@Service
public class ZipFattureService {
    @Autowired
    private FatturaRepository fatturaRepository;

    @Value("${fatture.zip.max-mb:14}")
    private double maxMb;

    /**
     * Parti in cui sono divisi i pdf del mese, con il numero di fatture e la dimensione dei pdf di ciascuna.
     */
    public List<ParteZipDTO> parti(int mese, int anno) throws IOException {
        List<List<Path>> parti = dividi(mese, anno);
        List<ParteZipDTO> risultato = new ArrayList<>();
        for (int i = 0; i < parti.size(); i++) {
            long byteParte = 0;
            for (Path pdf : parti.get(i)) byteParte += Files.size(pdf);
            risultato.add(new ParteZipDTO(i + 1, parti.size(), parti.get(i).size(), byteParte, nomeZip(mese, anno, i + 1, parti.size())));
        }
        return risultato;
    }

    /**
     * Nome del file zip della parte (numerata da 1).
     */
    public String nomeZip(int mese, int anno, int parte, int totale) {
        return String.format("fatture_%d_%02d_parte_%d_di_%d.zip", anno, mese, parte, totale);
    }

    /**
     * Scrive nello stream lo zip della parte (numerata da 1).
     */
    public void scriviZip(int mese, int anno, int parte, OutputStream out) throws IOException {
        List<List<Path>> parti = dividi(mese, anno);
        if (parte < 1 || parte > parti.size()) throw new NotFoundException("Parte " + parte + " non trovata");
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Path pdf : parti.get(parte - 1)) {
                zip.putNextEntry(new ZipEntry(pdf.getFileName().toString()));
                Files.copy(pdf, zip);
                zip.closeEntry();
            }
        }
    }

    /**
     * Pdf delle fatture del mese, in ordine di numero, divisi in gruppi riempiti fino alla dimensione massima.
     * I pdf sono già compressi: la dimensione dello zip è circa la somma dei file. Un pdf più grande del limite
     * finisce da solo in una parte.
     */
    private List<List<Path>> dividi(int mese, int anno) throws IOException {
        long limite = (long) (maxMb * 1024 * 1024);
        List<List<Path>> parti = new ArrayList<>();
        List<Path> corrente = new ArrayList<>();
        long byteCorrente = 0;
        for (Path pdf : pdfDelMese(mese, anno)) {
            long dimensione = Files.size(pdf);
            if (!corrente.isEmpty() && byteCorrente + dimensione > limite) {
                parti.add(corrente);
                corrente = new ArrayList<>();
                byteCorrente = 0;
            }
            corrente.add(pdf);
            byteCorrente += dimensione;
        }
        if (!corrente.isEmpty()) parti.add(corrente);
        if (parti.isEmpty()) throw new NotFoundException(String.format("Nessun pdf per le fatture di %02d/%d", mese, anno));
        return parti;
    }

    private List<Path> pdfDelMese(int mese, int anno) {
        return fatturaRepository.findByMeseAndAnnoOrderByNumeroFattura(mese, anno).stream()
                .map(Fattura::getPdf)
                .filter(src -> src != null && Files.isRegularFile(Path.of(src)))
                .map(Path::of)
                .toList();
    }
}
