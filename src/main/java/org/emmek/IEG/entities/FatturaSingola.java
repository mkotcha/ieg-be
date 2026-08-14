package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "fatture_singole")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FatturaSingola {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fattura_id", nullable = false)
    @JsonIgnore
    private Fattura fattura;

    @ManyToOne
    @JoinColumn(name = "fornitura_id", nullable = false)
    private Fornitura fornitura;

    @Column(name = "consumo_f1")
    private Double consumoF1;

    @Column(name = "consumo_f2")
    private Double consumoF2;

    @Column(name = "consumo_f3")
    private Double consumoF3;

    @Column(name = "consumo_f1r")
    private Double consumoF1r;

    @Column(name = "consumo_f2r")
    private Double consumoF2r;

    @Column(name = "consumo_f3r")
    private Double consumoF3r;

    @Column(name = "consumo_tot")
    private Double consumoTot;

    @Column(name = "consumo_tot_perdite")
    private Double consumoTotP;

    @Column(name = "consumo_tot_reattivo")
    private Double consumoTotR;

    @Column(name = "perditeF1")
    private Double perditeF1;

    @Column(name = "perditeF2")
    private Double perditeF2;

    @Column(name = "perditeF3")
    private Double perditeF3;

    @Column(name = "potenza_prelevata")
    private Double potenzaPrelevata;

    @Column(name = "totale_imposte")
    private Double totaleImposte;

    @Column(name = "totale_materia")
    private Double totaleMateria;

    @Column(name = "totale_trasporto")
    private Double totaleTrasporto;

    @Column(name = "totale_oneri")
    private Double totaleOneri;

    @Column(name = "totale_iva")
    private Double totaleIva;

    @Column(name = "totale_imponibile")
    private Double totaleImponibile;

    @ManyToOne
    @JoinColumn(name = "dispacciamento_id", nullable = false)
    private Dispacciamento dispacciamento;

    @ManyToOne
    @JoinColumn(name = "oneri_id", nullable = false)
    private Oneri oneri;

    @ManyToMany(mappedBy = "fattureSingole")
    private List<Lettura> letture;


    public void setConsumo() {

    }


}
