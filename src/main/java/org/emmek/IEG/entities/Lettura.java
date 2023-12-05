package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.TipoContatore;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "letture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Lettura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean isUtile;

    @Column(name = "tipo_lettura", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoLettura;

    @Column(name = "tipo_contatore", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoContatore;

    @Column(name = "data_lettura", nullable = false)
    private LocalDate dataLettura;

    private String raccolta;

    private String tipoDato;

    private String causaOstativa;

    private String validato;

    private String potMax;

    @Column(name = "ea_f1", nullable = false)
    private double eaF1;

    @Column(name = "ea_f2", nullable = false)
    private double eaF2;

    @Column(name = "ea_f3", nullable = false)
    private double eaF3;

    @Column(name = "er_f1", nullable = true)
    private double erF1;

    @Column(name = "er_f2", nullable = true)
    private double erF2;

    @Column(name = "er_f3", nullable = true)
    private double erF3;

    @Column(name = "pot_f1", nullable = true)
    private double potF1;

    @Column(name = "pot_f2", nullable = true)
    private double potF2;

    @Column(name = "pot_f3", nullable = true)
    private double potF3;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
    private Fornitura fornitura;

    @ManyToMany()
    @JoinTable(
            name = "letture_fatture",
            joinColumns = @JoinColumn(name = "lettura_id"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    private List<Fattura> fatture;

    @OneToMany(mappedBy = "lettura")
    private List<LetturaOraria> lettureOrarie;
}
