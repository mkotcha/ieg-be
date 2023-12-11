package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.TipoContatore;
import org.emmek.IEG.enums.TipoLettura;

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

    @Column(name = "is_utile", nullable = true)
    private boolean isUtile;

    @Column(name = "tipo_lettura", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoLettura tipoLettura;

    @Column(name = "tipo_contatore", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoContatore;

    @Column(name = "data_lettura", nullable = false)
    private LocalDate dataLettura;

    private String raccolta;

    @Column(name = "tipo_dato", nullable = true)
    private String tipoDato;

    @Column(name = "causa_ostativa", nullable = true)
    private String causaOstativa;

    private String validato;

    @Column(name = "pot_max", nullable = true)
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

    private String note;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
    @JsonIgnore
    private Fornitura fornitura;

    @ManyToMany()
    @JoinTable(
            name = "letture_fatture",
            joinColumns = @JoinColumn(name = "lettura_id"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    @JsonIgnore
    private List<Fattura> fatture;

    @OneToMany(mappedBy = "lettura")
    private List<LetturaOraria> lettureOrarie;
}
