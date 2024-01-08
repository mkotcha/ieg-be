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
