package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "fatture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Fattura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "numero_fattura", nullable = false)
    private String numeroFattura;

    @Column(name = "data_fattura", nullable = false)
    private LocalDate dataFattura;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;

    @Column(name = "consumo_tot")
    private Double consumoTot;

    @Column(name = "consumo_tot_perdite")
    private Double consumoTotP;

    @Column(name = "consumo_tot_reattivo")
    private Double consumoTotR;

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

    @Column(name = "xlsx")
    private String xlsx;

    @Column(name = "pdf")
    private String pdf;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "fattura")
    private List<FatturaSingola> fattureSingole;

    public void addFatturaSingola(FatturaSingola fatturaSingola) {
        if (fattureSingole == null) {
            fattureSingole = new java.util.ArrayList<>();
        }
        fattureSingole.add(fatturaSingola);

    }
}
