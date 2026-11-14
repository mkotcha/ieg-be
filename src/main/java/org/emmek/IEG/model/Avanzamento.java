package org.emmek.IEG.model;

/**
 * Avanzamento di un'operazione lunga, aggiornato da chi la esegue (anche da più thread insieme).
 */
public interface Avanzamento {

    /**
     * Inizia una nuova fase; totale 0 = durata non nota (barra indeterminata).
     */
    void fase(String nome, int totale);

    /**
     * Un elemento della fase corrente è stato completato (con o senza errore).
     */
    void avanza();

    /**
     * Errore su un singolo elemento: non interrompe l'operazione.
     */
    void errore(String messaggio);

    /**
     * Per chiamare i servizi senza seguire l'avanzamento.
     */
    Avanzamento NESSUNO = new Avanzamento() {
        @Override
        public void fase(String nome, int totale) {
        }

        @Override
        public void avanza() {
        }

        @Override
        public void errore(String messaggio) {
        }
    };
}
