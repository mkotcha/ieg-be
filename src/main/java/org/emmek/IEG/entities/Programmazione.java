package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "programmazione")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Programmazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "oneri_programmazione", nullable = false)
    private double oneriProgrammazione;

    @Column(name = "commercializzazione", nullable = false)
    private double commercializzazione;

    @OneToMany(mappedBy = "programmazione")
    private List<Fornitura> forniture;
}
