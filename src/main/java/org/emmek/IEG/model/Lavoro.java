package org.emmek.IEG.model;

import lombok.Getter;
import org.emmek.IEG.enums.StatoLavoro;
import org.emmek.IEG.enums.TipoLavoro;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Operazione lunga eseguita in background: stato e avanzamento letti dal fe con il polling di GET /api/lavori/{id}.
 * Aggiornata dal thread del lavoro (e dai suoi thread di supporto), letta dai thread delle richieste.
 */
@Getter
public class Lavoro implements Avanzamento {

    // messaggi d'errore conservati: gli altri sono solo contati
    private static final int MAX_MESSAGGI = 200;

    private final String id = UUID.randomUUID().toString();
    private final TipoLavoro tipo;
    private final String descrizione;
    private final LocalDateTime inizio = LocalDateTime.now();
    private volatile LocalDateTime fine;
    private volatile StatoLavoro stato = StatoLavoro.IN_CORSO;
    private volatile String fase = "In attesa";
    private volatile int totale;
    private final AtomicInteger fatti = new AtomicInteger();
    private final AtomicInteger errori = new AtomicInteger();
    private final List<String> messaggi = new ArrayList<>();
    // errore che ha interrotto il lavoro
    private volatile String errore;
    // esito del lavoro, disponibile a lavoro completato
    private volatile Object risultato;

    public Lavoro(TipoLavoro tipo, String descrizione) {
        this.tipo = tipo;
        this.descrizione = descrizione;
    }

    @Override
    public void fase(String nome, int totale) {
        this.fatti.set(0);
        this.totale = totale;
        this.fase = nome;
    }

    @Override
    public void avanza() {
        fatti.incrementAndGet();
    }

    @Override
    public void errore(String messaggio) {
        errori.incrementAndGet();
        synchronized (messaggi) {
            if (messaggi.size() < MAX_MESSAGGI) messaggi.add(messaggio);
        }
    }

    public int getFatti() {
        return fatti.get();
    }

    public int getErrori() {
        return errori.get();
    }

    public List<String> getMessaggi() {
        synchronized (messaggi) {
            return List.copyOf(messaggi);
        }
    }

    public boolean isInCorso() {
        return stato == StatoLavoro.IN_CORSO;
    }

    public void completa(Object risultato) {
        this.risultato = risultato;
        this.fine = LocalDateTime.now();
        this.stato = StatoLavoro.COMPLETATO;
    }

    public void fallisci(String errore) {
        this.errore = errore;
        this.fine = LocalDateTime.now();
        this.stato = StatoLavoro.FALLITO;
    }
}
