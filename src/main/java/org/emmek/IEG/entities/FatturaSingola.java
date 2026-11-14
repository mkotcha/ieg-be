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
    @JoinColumn(name = "fornitura_pod", nullable = false)
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

    // energia reattiva capacitiva (immessa) per fascia, kVArh
    @Column(name = "consumo_f1rc")
    private Double consumoF1rc;

    @Column(name = "consumo_f2rc")
    private Double consumoF2rc;

    @Column(name = "consumo_f3rc")
    private Double consumoF3rc;

    @Column(name = "consumo_tot_reattivo_immesso")
    private Double consumoTotRc;

    // consumo totale calcolato solo come ultima lettura - prima lettura, per verifica di consumoTot
    @Column(name = "consumo_tot_verifica")
    private Double consumoTotVerifica;

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

    // prezzi energia applicati (PUN o prezzo fisso dell'offerta) e potenza usata per le quote potenza
    @Column(name = "prezzo_f1")
    private Double prezzoF1;

    @Column(name = "prezzo_f2")
    private Double prezzoF2;

    @Column(name = "prezzo_f3")
    private Double prezzoF3;

    // potenza impegnata se sotto i 16 kW, altrimenti il picco prelevato
    @Column(name = "potenza_oneri")
    private Double potenzaOneri;

    // valori dello scontrino dell'energia (pagina 2)
    @Column(name = "spesa_vendita")
    private Double spesaVendita;

    @Column(name = "spesa_rete")
    private Double spesaRete;

    @Column(name = "spesa_fissa_vendita")
    private Double spesaFissaVendita;

    @Column(name = "spesa_fissa_rete")
    private Double spesaFissaRete;

    @Column(name = "spesa_potenza_rete")
    private Double spesaPotenzaRete;

    // €/kWh
    @Column(name = "spesa_media")
    private Double spesaMedia;

    // €/kWh
    @Column(name = "spesa_vendita_media")
    private Double spesaVenditaMedia;

    // €/kWh
    @Column(name = "spesa_rete_media")
    private Double spesaReteMedia;

    // €/kW/mese
    @Column(name = "spesa_potenza_rete_media")
    private Double spesaPotenzaReteMedia;

    // tutte le altre partite (le soggette a iva sono anche nell'imponibile)
    @Column(name = "totale_partite")
    private Double totalePartite;

    // imponibile + iva + altre partite esenti iva
    @Column(name = "totale_da_pagare")
    private Double totaleDaPagare;

    @ManyToOne
    @JoinColumn(name = "dispacciamento_id", nullable = false)
    private Dispacciamento dispacciamento;

    @ManyToOne
    @JoinColumn(name = "oneri_id", nullable = false)
    private Oneri oneri;

    @ManyToMany(mappedBy = "fattureSingole")
    private List<Lettura> letture;

    @OneToMany(mappedBy = "fatturaSingola", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordine")
    private List<VoceFatturaSingola> voci = new java.util.ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "lettura_iniziale_id")
    private Lettura letturaIniziale;

    @ManyToOne
    @JoinColumn(name = "lettura_finale_id")
    private Lettura letturaFinale;


    public void setConsumo() {

    }


}
