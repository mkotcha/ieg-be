package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Riga del csv di riepilogo della fattura del fornitore: una voce di un POD per un periodo.
 * Il pod non è una FK: il fornitore può fatturare POD che non abbiamo in anagrafica.
 * mese/anno sono la competenza (mese di inizioPeriodo): i conguagli (storno della stima + rifatturazione sul reale)
 * arrivano in bollette successive ma si sommano al mese a cui si riferiscono.
 */
@Entity
@Table(name = "voci_fatture_fornitore")
@NoArgsConstructor
@Getter
@Setter
public class VoceFatturaFornitore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fattura_fornitore_id", nullable = false)
    @JsonIgnore
    private FatturaFornitore fatturaFornitore;

    // numero di riga nel csv
    @Column(nullable = false)
    private int riga;

    private String pod;

    @Column(name = "nr_utenza")
    private String nrUtenza;

    @Column(name = "classe_utenza")
    private String classeUtenza;

    @Column(name = "inizio_periodo")
    private LocalDate inizioPeriodo;

    @Column(name = "fine_periodo")
    private LocalDate finePeriodo;

    @Column(nullable = false)
    private int mese;

    @Column(nullable = false)
    private int anno;

    @Column(name = "consumo_reale")
    private Boolean consumoReale;

    private String unita;

    private Double quantita;

    private Double unitario;

    @Column(name = "perc_iva")
    private Double percIva;

    @Column(nullable = false)
    private Double importo;

    private String descrizione;

    @Column(name = "cod_offerta")
    private String codOfferta;

    @Column(name = "cod_tariffa")
    private String codTariffa;

    @Column(name = "nr_articolo")
    private String nrArticolo;

    @Column(name = "tipo_ricalcolo")
    private String tipoRicalcolo;

    @Column(name = "motivo_ricalcolo")
    private String motivoRicalcolo;

    @Column(name = "tipo_mercato")
    private String tipoMercato;

    @Column(name = "potenza_impegnata")
    private Double potenzaImpegnata;

    @Column(name = "potenza_disponibile")
    private Double potenzaDisponibile;

    @Column(name = "unitario_originale")
    private Double unitarioOriginale;

    @Column(name = "unitario_aggiuntivo")
    private Double unitarioAggiuntivo;

    private Double spread;

    // data di apertura dell'utenza presso il fornitore (inizio fornitura)
    @Column(name = "data_apertura")
    private LocalDate dataApertura;

    // es. "ALTRI USI BT RE": contiene il livello di tensione (BT/MT/AT)
    @Column(name = "tipo_utenza")
    private String tipoUtenza;

    public boolean isRicalcolo() {
        return tipoRicalcolo != null && !tipoRicalcolo.isBlank();
    }

    public boolean isPerdite() {
        return descrizione != null && descrizione.toLowerCase().contains("perdite");
    }

    // numero della bolletta in cui è arrivata la riga (non serializza la fattura intera)
    public String getNumeroFattura() {
        return fatturaFornitore != null ? fatturaFornitore.getNumero() : null;
    }
}
