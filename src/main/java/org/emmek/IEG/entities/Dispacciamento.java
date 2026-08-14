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
    private Long id;

    @Column(name = "costo_am", nullable = false)
    private Double costoAm;

    @Column(name = "dis", nullable = false)
    private Double dis;

    @Column(name = "capacita")
    private Double capacita;

    @Column(name = "sbilanciamento")
    private Double sbilanciamento;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;
}
