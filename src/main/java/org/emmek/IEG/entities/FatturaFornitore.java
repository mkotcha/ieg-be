package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fattura del fornitore all'ingrosso verso IEG, importata dal csv di riepilogo. Le righe sono in
 * {@link VoceFatturaFornitore}; reimportando la stessa bolletta le righe vengono sostituite.
 */
@Entity
@Table(name = "fatture_fornitore")
@NoArgsConstructor
@Getter
@Setter
public class FatturaFornitore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(name = "data_emissione")
    private LocalDate dataEmissione;

    @Column(name = "data_scadenza")
    private LocalDate dataScadenza;

    @Column(name = "nome_file")
    private String nomeFile;

    @Column(name = "data_import", nullable = false)
    private LocalDateTime dataImport;

    @Column(nullable = false)
    private int righe;

    @Column(name = "totale_imponibile", nullable = false)
    private Double totaleImponibile;
}
