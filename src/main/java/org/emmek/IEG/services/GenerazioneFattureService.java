package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.Cliente;
import org.emmek.IEG.entities.Fattura;
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
 * Genera le fatture di tutti i clienti attivi per un mese, in tre fasi:
 * <ol>
 *     <li>in parallelo (solo Java): calcolo e salvataggio di ogni fattura, compilazione delle pagine con POI;
 *     ogni cliente ha una propria transazione e un errore non blocca gli altri</li>
 *     <li>in sequenza: composizione di xlsx e pdf di tutte le fatture con una sola istanza di Excel
 *     (più istanze in parallelo si rallentano a vicenda e a volte cadono)</li>
 *     <li>registrazione dei file sulle fatture composte</li>
 * </ol>
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
    private ExcelService excelService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // thread per il calcolo delle fatture e la compilazione delle pagine
    @Value("${fatture.generazione.thread:4}")
    private int thread;

    private record Calcolata(Long clienteId, String ragioneSociale, String numeroFattura, double totale,
                             ExcelService.LavoroFattura lavoro, long millisecondi) {
    }

    public List<EsitoFatturaDTO> generaFatture(int mese, int anno) {
        List<Long> clienti = clienteRepository.findByIsActiveTrueOrderById().stream()
                .filter(c -> c.getForniture() != null && !c.getForniture().isEmpty())
                .map(Cliente::getId)
                .toList();
        log.info("Generazione fatture {}/{}: {} clienti attivi, {} thread", mese, anno, clienti.size(), thread);
        long inizio = System.currentTimeMillis();
        List<EsitoFatturaDTO> esiti = new ArrayList<>();

        // 1. calcolo e pagine, in parallelo
        List<Calcolata> calcolate = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(thread);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Long id : clienti) {
                futures.add(executor.submit(() -> calcola(id, mese, anno)));
            }
            for (Future<Object> future : futures) {
                Object risultato = attendi(future);
                if (risultato instanceof Calcolata c) calcolate.add(c);
                else esiti.add((EsitoFatturaDTO) risultato);
            }
        } finally {
            executor.shutdownNow();
        }
        log.info("Fatture {}/{} calcolate in {} s: {} da comporre, {} errori", mese, anno,
                (System.currentTimeMillis() - inizio) / 1000, calcolate.size(), esiti.size());

        // 2. composizione di xlsx e pdf, una istanza di Excel per tutte
        long inizioComposizione = System.currentTimeMillis();
        List<String> errori;
        try {
            errori = excelService.componiFatture(calcolate.stream().map(Calcolata::lavoro).toList());
        } catch (IOException | RuntimeException e) {
            log.error("Composizione delle fatture {}/{} non riuscita: {}", mese, anno, e.getMessage());
            errori = calcolate.stream().map(c -> "Composizione non riuscita: " + e.getMessage()).toList();
        }
        long millisComposizione = calcolate.isEmpty() ? 0 : (System.currentTimeMillis() - inizioComposizione) / calcolate.size();

        // 3. file registrati sulle fatture composte
        for (int i = 0; i < calcolate.size(); i++) {
            Calcolata c = calcolate.get(i);
            String errore = errori.get(i);
            if (errore == null) {
                fatturaRepository.findById(c.lavoro().fatturaId()).ifPresent(f -> {
                    f.setXlsx(c.lavoro().xlsx());
                    f.setPdf(c.lavoro().pdf());
                    fatturaRepository.save(f);
                });
            } else {
                log.error("Fattura {} non composta: {}", c.numeroFattura(), errore);
            }
            esiti.add(new EsitoFatturaDTO(c.clienteId(), c.ragioneSociale(), c.numeroFattura(), c.totale(),
                    c.millisecondi() + millisComposizione, errore));
        }

        esiti.sort(Comparator.comparing(EsitoFatturaDTO::clienteId));
        long numeroErrori = esiti.stream().filter(e -> e.errore() != null).count();
        log.info("Generazione fatture {}/{} terminata in {} s: {} generate, {} errori", mese, anno,
                (System.currentTimeMillis() - inizio) / 1000, esiti.size() - numeroErrori, numeroErrori);
        return esiti;
    }

    /**
     * Calcola e salva la fattura del cliente e ne compila le pagine.
     *
     * @return una {@link Calcolata} o, in caso di errore, l'{@link EsitoFatturaDTO} con il messaggio
     */
    private Object calcola(Long clienteId, int mese, int anno) {
        long inizio = System.currentTimeMillis();
        try {
            return transactionTemplate.execute(status -> {
                Cliente cliente = clienteRepository.findById(clienteId).orElseThrow();
                Fattura fattura = fatturaService.calcolaFattura(cliente, mese, anno);
                double partite = fattura.getFattureSingole().stream()
                        .mapToDouble(fs -> fs.getTotalePartite() != null ? fs.getTotalePartite() : 0).sum();
                try {
                    ExcelService.LavoroFattura lavoro = excelService.preparaFattura(fattura);
                    return new Calcolata(clienteId, cliente.getRagioneSociale(), fattura.getNumeroFattura(),
                            fattura.getTotaleImponibile() + fattura.getTotaleIva() + partite, lavoro,
                            System.currentTimeMillis() - inizio);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            log.error("Fattura cliente {} {}/{} non generata: {}", clienteId, mese, anno, e.getMessage());
            String ragioneSociale = clienteRepository.findById(clienteId).map(Cliente::getRagioneSociale).orElse(null);
            return new EsitoFatturaDTO(clienteId, ragioneSociale, null, null,
                    System.currentTimeMillis() - inizio, e.getMessage());
        }
    }

    private static Object attendi(Future<Object> future) {
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
