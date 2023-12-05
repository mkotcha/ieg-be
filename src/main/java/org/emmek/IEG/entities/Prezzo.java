package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "prezzi")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Prezzo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "f1", nullable = true)
    private double f1;

    @Column(name = "f2", nullable = true)
    private double f2;

    @Column(name = "f3", nullable = true)
    private double f3;

    @Column(name = "is_pun", nullable = true)
    private boolean isPun;

    @Column(name = "maggiorazione", nullable = true)
    private double maggiorazione;

    @Column(name = "spread", nullable = true)
    private double spread;

    @OneToMany(mappedBy = "prezzo")
    private List<Fornitura> forniture;
}
