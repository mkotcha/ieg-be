package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.GruppoScontrino;
import org.emmek.IEG.enums.SezioneFattura;

import java.time.LocalDate;

/**
 * Riga del dettaglio di una fattura singola (allegato di dettaglio della fattura): importo = unitario * quantita.
 */
@Entity
@Table(name = "voci_fatture_singole")
@NoArgsConstructor
@Getter
@Setter
public class VoceFatturaSingola {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fattura_singola_id", nullable = false)
    @JsonIgnore
    private FatturaSingola fatturaSingola;

    /**
     * Identificativo della voce (es. energia_f1): usato per raggrupparle nel pdf e per il confronto col fornitore.
     */
    @Column(nullable = false)
    private String codice;

    private String descrizione;

    @Enumerated(EnumType.STRING)
    private SezioneFattura sezione;

    /**
     * Voce dello scontrino (pagina 2) in cui confluisce l'importo; null se non confluisce in nessuna.
     */
    @Column(name = "gruppo_scontrino")
    @Enumerated(EnumType.STRING)
    private GruppoScontrino gruppoScontrino;

    private LocalDate dal;

    private LocalDate al;

    private String unita;

    private Double unitario;

    private Double quantita;

    private Double importo;

    private Double iva;

    @Column(name = "ordine")
    private int ordine;
}
