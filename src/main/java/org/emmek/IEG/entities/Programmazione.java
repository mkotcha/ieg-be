package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "programmazione")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Programmazione {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "oneri_programmazione", nullable = false)
    private double oneriProgrammazione;

    @Column(name = "commercializzazione", nullable = false)
    private double commercializzazione;

    @OneToMany(mappedBy = "programmazione")
    private List<Fornitura> forniture;
}