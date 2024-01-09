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
    private long id;

    @ManyToOne
    @JoinColumn(name = "fattura_id", nullable = false)
    @JsonIgnore
    private Fattura fattura;

    @ManyToOne
    @JoinColumn(name = "fornitura_id", nullable = false)
    private Fornitura fornitura;

    @Column(name = "consumo_f1")
    private double consumoF1;

    @Column(name = "consumo_f2")
    private double consumoF2;

    @Column(name = "consumo_f3")
    private double consumoF3;

    @Column(name = "consumo_f1r")
    private double consumoF1r;

    @Column(name = "consumo_f2r")
    private double consumoF2r;

    @Column(name = "consumo_f3r")
    private double consumoF3r;

    @Column(name = "consumo_tot")
    private double consumoTot;

    @Column(name = "consumo_tot_perdite")
    private double consumoTotP;

    @Column(name = "consumo_tot_reattivo")
    private double consumoTotR;

    @Column(name = "perditeF1")
    private double perditeF1;

    @Column(name = "perditeF2")
    private double perditeF2;

    @Column(name = "perditeF3")
    private double perditeF3;

    @Column(name = "potenza_prelevata")
    private double potenzaPrelevata;

    @Column(name = "totale_imposte")
    private double totaleImposte;

    @Column(name = "totale_materia")
    private double totaleMateria;

    @Column(name = "totale_trasporto")
    private double totaleTrasporto;

    @Column(name = "totale_oneri")
    private double totaleOneri;

    @Column(name = "totale_imponibile")
    private double totaleImponibile;

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
