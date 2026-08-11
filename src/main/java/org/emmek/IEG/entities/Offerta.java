package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.TipoOfferta;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "offerte")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Offerta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "codice")
    private String codice;

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoOfferta tipo;

    @Column(name = "f1")
    private Double f1;

    @Column(name = "f2")
    private Double f2;

    @Column(name = "f3")
    private Double f3;

    @Column(name = "is_pun")
    private boolean isPun;

    @Column(name = "is_c_mag")
    private boolean isCapacitaMaggiorazione;

    @Column(name = "maggiorazione")
    private Double maggiorazione;

    @Column(name = "has_spread")
    private boolean hasSpread;

    @Column(name = "spread")
    private Double spread;

    @Column(name = "ocv")
    private Double ocv;

    @Column(name = "ocf")
    private Double ocf;

    @Column(name = "descrizione")
    private String descrizione;

    @Column(name = "data_inizio")
    private LocalDate dataInizio;

    @Column(name = "data_fine")
    private LocalDate dataFine;

    @OneToMany(mappedBy = "offerta")
    @JsonIgnore
    private List<Fornitura> forniture;
}
