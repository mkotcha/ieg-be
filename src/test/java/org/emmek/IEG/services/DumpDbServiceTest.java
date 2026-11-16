package org.emmek.IEG.services;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DumpDbServiceTest {

    @Test
    void comandoDivisoSugliSpazi() {
        assertEquals(List.of("podman", "exec", "-e", "PGPASSWORD", "postgres", "pg_dump"),
                DumpDbService.dividi("  podman exec -e PGPASSWORD   postgres pg_dump "));
    }

    @Test
    void percorsoTraVirgoletteResoIntero() {
        assertEquals(List.of("C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe"),
                DumpDbService.dividi("\"C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe\""));
    }
}
