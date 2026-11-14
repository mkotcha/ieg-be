package org.emmek.IEG.services;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.enums.TipoLavoro;
import org.emmek.IEG.exceptions.BadRequestException;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.model.Lavoro;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Esegue in background le operazioni lunghe (generazione delle fatture, import delle letture) e ne tiene lo stato
 * in memoria, letto dal fe con il polling. Ne gira al più una per tipo. Lo stato si perde al riavvio del backend.
 */
@Service
@Slf4j
public class LavoroService {

    // lavori terminati conservati per poterne rileggere l'esito (es. dopo aver ricaricato la pagina)
    private static final long ORE_CONSERVAZIONE = 12;

    private final Map<String, Lavoro> lavori = new ConcurrentHashMap<>();
    private final AtomicInteger contatoreThread = new AtomicInteger();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "lavoro-" + contatoreThread.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    /**
     * Avvia il compito in background e restituisce subito il lavoro; il valore restituito dal compito diventa il
     * risultato del lavoro, un'eccezione lo fa fallire.
     *
     * @throws BadRequestException se un lavoro dello stesso tipo è già in corso
     */
    public synchronized Lavoro avvia(TipoLavoro tipo, String descrizione, Function<Lavoro, Object> compito) {
        pulisci();
        ultimo(tipo).filter(Lavoro::isInCorso).ifPresent(l -> {
            throw new BadRequestException("C'è già un'operazione in corso: " + l.getDescrizione());
        });
        Lavoro lavoro = new Lavoro(tipo, descrizione);
        lavori.put(lavoro.getId(), lavoro);
        log.info("Avvio lavoro {}: {}", lavoro.getId(), descrizione);
        executor.submit(() -> {
            try {
                lavoro.completa(compito.apply(lavoro));
                log.info("Lavoro {} completato: {}", lavoro.getId(), descrizione);
            } catch (Throwable e) {
                log.error("Lavoro {} fallito: {}", lavoro.getId(), descrizione, e);
                lavoro.fallisci(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
        return lavoro;
    }

    public Lavoro findById(String id) {
        Lavoro lavoro = lavori.get(id);
        if (lavoro == null) throw new NotFoundException("Operazione " + id + " non trovata (il backend è stato riavviato?)");
        return lavoro;
    }

    /**
     * Il lavoro più recente del tipo, in corso o terminato.
     */
    public Optional<Lavoro> ultimo(TipoLavoro tipo) {
        return lavori.values().stream()
                .filter(l -> l.getTipo() == tipo)
                .max(Comparator.comparing(Lavoro::getInizio));
    }

    private void pulisci() {
        LocalDateTime limite = LocalDateTime.now().minusHours(ORE_CONSERVAZIONE);
        lavori.values().removeIf(l -> l.getFine() != null && l.getFine().isBefore(limite));
    }

    @PreDestroy
    public void chiudi() {
        executor.shutdownNow();
    }
}
