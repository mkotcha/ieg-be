package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.model.Avanzamento;
import org.emmek.IEG.payloads.EsitoFatturaDTO;
import org.emmek.IEG.repositories.ClienteRepository;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Genera le fatture di tutti i clienti attivi per un mese: calcolo, salvataggio e pdf di ogni fattura, in parallelo
 * (solo Java). Ogni cliente ha una propria transazione e un errore non blocca gli altri.
 */
@Service
@Slf4j
public class GenerazioneFattureService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FatturaRepository fatturaRepository;

    @Autowired
    private FatturaService fatturaService;

    @Autowired
    private PdfFatturaService pdfFatturaService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // thread per il calcolo delle fatture e la generazione dei pdf
    @Value("${fatture.generazione.thread:4}")
    private int thread;

    public List<EsitoFatturaDTO> generaFatture(int mese, int anno) {
        return generaFatture(mese, anno, Avanzamento.NESSUNO);
    }

    /**
     * Come sopra, aggiornando l'avanzamento (un passo per cliente).
     */
    public List<EsitoFatturaDTO> generaFatture(int mese, int anno, Avanzamento avanzamento) {
        // in transazione: le forniture sono lazy e in background non c'è la sessione della richiesta
        List<Long> clienti = transactionTemplate.execute(status -> clienteRepository.findByIsActiveTrueOrderById().stream()
                .filter(c -> c.getForniture() != null && !c.getForniture().isEmpty())
                .map(Cliente::getId)
                .toList());
        log.info("Generazione fatture {}/{}: {} clienti attivi, {} thread", mese, anno, clienti.size(), thread);
        long inizio = System.currentTimeMillis();
        List<EsitoFatturaDTO> esiti = new ArrayList<>();

        avanzamento.fase("Generazione fatture", clienti.size());
        ExecutorService executor = Executors.newFixedThreadPool(thread);
        try {
            List<Future<EsitoFatturaDTO>> futures = new ArrayList<>();
            for (Long id : clienti) {
                // avanzamento aggiornato appena il cliente è pronto, non nell'ordine di attesa dei future
                futures.add(executor.submit(() -> {
                    EsitoFatturaDTO esito = genera(id, mese, anno);
                    if (esito.errore() != null) avanzamento.errore(esito.ragioneSociale() + ": " + esito.errore());
                    avanzamento.avanza();
                    return esito;
                }));
            }
            for (Future<EsitoFatturaDTO> future : futures) esiti.add(attendi(future));
        } finally {
            executor.shutdownNow();
        }

        esiti.sort(Comparator.comparing(EsitoFatturaDTO::clienteId));
        long numeroErrori = esiti.stream().filter(e -> e.errore() != null).count();
        log.info("Generazione fatture {}/{} terminata in {} s: {} generate, {} errori", mese, anno,
                (System.currentTimeMillis() - inizio) / 1000, esiti.size() - numeroErrori, numeroErrori);
        return esiti;
    }

    /**
     * Calcola e salva la fattura del cliente e ne genera il pdf. Se il pdf non riesce la fattura resta salvata e
     * l'esito riporta l'errore.
     */
    private EsitoFatturaDTO genera(Long clienteId, int mese, int anno) {
        long inizio = System.currentTimeMillis();
        try {
            return transactionTemplate.execute(status -> {
                Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
                Fattura fattura = fatturaService.calcolaFattura(cliente, mese, anno);
                double totale = fattura.getFattureSingole().stream()
                        .mapToDouble(fs -> fs.getTotaleDaPagare() != null ? fs.getTotaleDaPagare() : 0).sum();
                String errore = null;
                try {
                    fattura.setPdf(pdfFatturaService.generaPdf(fattura));
                    fattura.setXlsx(null);
                    fatturaRepository.save(fattura);
                } catch (IOException | RuntimeException e) {
                    log.error("Pdf della fattura {} non generato", fattura.getNumeroFattura(), e);
                    errore = "Pdf non generato: " + e.getMessage();
                }
                return new EsitoFatturaDTO(clienteId, cliente.getRagioneSociale(), fattura.getNumeroFattura(), totale,
                        System.currentTimeMillis() - inizio, errore);
            });
        } catch (RuntimeException e) {
            log.error("Fattura cliente {} {}/{} non generata: {}", clienteId, mese, anno, e.getMessage());
            String ragioneSociale = clienteRepository.findById(clienteId).map(Cliente::getRagioneSociale).orElse(null);
            return new EsitoFatturaDTO(clienteId, ragioneSociale, null, null,
                    System.currentTimeMillis() - inizio, e.getMessage());
        }
    }

    private static EsitoFatturaDTO attendi(Future<EsitoFatturaDTO> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Generazione fatture interrotta", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
