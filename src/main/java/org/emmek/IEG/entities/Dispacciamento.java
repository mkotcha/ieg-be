package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dispacciamento")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Dispacciamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "capacita", nullable = false)
    private double capacita;

    @Column(name = "eolico", nullable = false)
    private double eolico;

    @Column(name = "costo_am", nullable = false)
    private double costoAm;

    @Column(name = "dis", nullable = false)
    private double dis;

    @Column(name = "int", nullable = false)
    private double int73;

    @Column(name = "msd", nullable = false)
    private double msd;

    @Column(name = "sicurezza", nullable = false)
    private double sicurezza;

    @Column(name = "trasmissione", nullable = false)
    private double trasmissione;

    @Column(name = "trimestre", nullable = false)
    private int trimestre;

    @Column(name = "anno", nullable = false)
    private int anno;
}
