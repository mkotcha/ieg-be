package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.TipoContatore;
import org.emmek.IEG.enums.TipoLettura;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "letture_id_seq")
    @SequenceGenerator(name = "letture_id_seq", sequenceName = "letture_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "is_utile")
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

    @Column(name = "tipo_dato")
    private String tipoDato;

    @Column(name = "causa_ostativa")
    private String causaOstativa;

    private String validato;

    @Column(name = "pot_max")
    private String potMax;

    @Column(name = "ea_f1", nullable = false)
    private Double eaF1;
    @Column(name = "ea_f2", nullable = false)
    private Double eaF2;
    @Column(name = "ea_f3", nullable = false)
    private Double eaF3;

    @Column(name = "er_f1")
    private Double erF1;
    @Column(name = "er_f2")
    private Double erF2;
    @Column(name = "er_f3")
    private Double erF3;

    @Column(name = "pot_f1")
    private Double potF1;
    @Column(name = "pot_f2")
    private Double potF2;
    @Column(name = "pot_f3")
    private Double potF3;

    @Column(name = "ka")
    private Double ka;
    @Column(name = "kr")
    private Double kr;
    @Column(name = "kp")
    private Double kp;

    private String note;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
//    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Fornitura fornitura;

    @ManyToMany()
    @JoinTable(
            name = "letture_fatture_singole",
            joinColumns = @JoinColumn(name = "lettura_id"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    @JsonIgnore
    private List<FatturaSingola> fattureSingole;

}
