package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Dump del database con pg_dump in formato custom (-Fc, si ripristina con pg_restore).
 * Il comando è configurabile ({@code db.dump.comando}) perché pg_dump può essere installato sulla macchina
 * ("pg_dump", o il percorso completo su Windows tra virgolette) o solo nel container di Postgres
 * ("docker exec -e PGPASSWORD postgres pg_dump"). Host, porta, utente e database vengono dal datasource,
 * la password è passata in PGPASSWORD.
 */
@Service
@Slf4j
public class DumpDbService {
    private static final long TIMEOUT_MINUTI = 10;

    @Value("${db.dump.comando:pg_dump}")
    private String comando;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String utente;

    @Value("${spring.datasource.password}")
    private String password;

    /**
     * Esegue il dump in un file temporaneo e lo restituisce; il chiamante lo cancella dopo averlo inviato.
     * Il file si scrive per intero prima di inviarlo: se pg_dump fallisce si ha un errore e non un dump troncato.
     */
    public Path dump() throws IOException, InterruptedException {
        // jdbc:postgresql://host:porta/database
        URI uri = URI.create(url.substring("jdbc:".length()));
        String database = uri.getPath().substring(1);

        List<String> argomenti = new ArrayList<>(dividi(comando));
        argomenti.addAll(List.of("-Fc", "-h", uri.getHost(), "-p", String.valueOf(uri.getPort() > 0 ? uri.getPort() : 5432),
                "-U", utente, "-d", database));

        Path dump = Files.createTempFile("ieg-dump-", ".dump");
        Path errori = Files.createTempFile("ieg-dump-", ".log");
        try {
            ProcessBuilder pb = new ProcessBuilder(argomenti)
                    .redirectOutput(dump.toFile())
                    .redirectError(errori.toFile());
            pb.environment().put("PGPASSWORD", password);
            log.info("Dump del database {} con: {}", database, String.join(" ", argomenti));
            // errori come IllegalStateException: l'handler le restituisce con il messaggio, visibile nel fe
            Process processo;
            try {
                processo = pb.start();
            } catch (IOException e) {
                throw new IllegalStateException("Impossibile eseguire pg_dump con il comando \"" + comando
                        + "\" (db.dump.comando): " + e.getMessage(), e);
            }
            if (!processo.waitFor(TIMEOUT_MINUTI, TimeUnit.MINUTES)) {
                processo.destroyForcibly();
                throw new IllegalStateException("pg_dump non ha finito entro " + TIMEOUT_MINUTI + " minuti");
            }
            if (processo.exitValue() != 0) {
                String messaggio = Files.readString(errori, StandardCharsets.UTF_8).trim();
                throw new IllegalStateException("pg_dump terminato con codice " + processo.exitValue() + ": " + messaggio);
            }
            return dump;
        } catch (IOException | InterruptedException | RuntimeException e) {
            Files.deleteIfExists(dump);
            throw e;
        } finally {
            Files.deleteIfExists(errori);
        }
    }

    /**
     * Divide il comando sugli spazi, tenendo insieme le parti tra virgolette (percorsi Windows con spazi).
     */
    static List<String> dividi(String comando) {
        List<String> parti = new ArrayList<>();
        StringBuilder corrente = new StringBuilder();
        boolean virgolette = false;
        for (char c : comando.trim().toCharArray()) {
            if (c == '"') {
                virgolette = !virgolette;
            } else if (Character.isWhitespace(c) && !virgolette) {
                if (!corrente.isEmpty()) {
                    parti.add(corrente.toString());
                    corrente.setLength(0);
                }
            } else {
                corrente.append(c);
            }
        }
        if (!corrente.isEmpty()) parti.add(corrente.toString());
        return parti;
    }
}
