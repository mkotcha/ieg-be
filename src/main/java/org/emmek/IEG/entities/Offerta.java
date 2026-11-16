package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.ModalitaProgrammazione;
import org.emmek.IEG.enums.TipoOfferta;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "offerte")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Offerta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "codice")
    private String codice;

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoOfferta tipo;

    @Column(name = "f1")
    private Double f1;

    @Column(name = "f2")
    private Double f2;

    @Column(name = "f3")
    private Double f3;

    @Column(name = "is_pun")
    private boolean isPun;

    @Column(name = "is_c_mag")
    private boolean isCapacitaMaggiorazione;

    @Column(name = "maggiorazione")
    private Double maggiorazione;

    // se attivo in fattura si aggiunge la maggiorazione sbilanciamento di 0,01 €/kWh (righe proprie, è margine)
    @Column(name = "maggiorazione_sbilanciamento", nullable = false)
    private boolean maggiorazioneSbilanciamento;

    @Column(name = "has_spread")
    private boolean hasSpread;

    @Column(name = "spread")
    private Double spread;

    @Column(name = "ocv")
    private Double ocv;

    // come si applica ocv: €/kWh sul consumo con perdite (OCV) o percentuale sulla spesa energia (CLASSIC)
    @Column(name = "modalita_programmazione")
    @Enumerated(EnumType.STRING)
    private ModalitaProgrammazione modalitaProgrammazione = ModalitaProgrammazione.OCV;

    @Column(name = "ocf")
    private Double ocf;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "data_inizio")
    private LocalDate dataInizio;

    @Column(name = "data_fine")
    private LocalDate dataFine;

    @OneToMany(mappedBy = "offerta")
    @JsonIgnore
    private List<Fornitura> forniture;

    /**
     * Spread da applicare in fattura: vale solo se has_spread è attivo, altrimenti 0.
     */
    @JsonIgnore
    public double getSpreadEffettivo() {
        return hasSpread && spread != null ? spread : 0;
    }
}
