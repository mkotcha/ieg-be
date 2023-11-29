package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.TipoContatore;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "letture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Lettura {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    private boolean isUtile;

    @Column(name = "tipo_lettura", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoLettura;

    @Column(name = "tipo_contatore", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoContatore;

    @Column(name = "data_lettura", nullable = false)
    private LocalDate dataLettura;

    @Column(name = "f1", nullable = false)
    private double f1;

    @Column(name = "f2", nullable = false)
    private double f2;

    @Column(name = "f3", nullable = false)
    private double f3;

    @Column(name = "f1r", nullable = true)
    private double f1r;

    @Column(name = "f2r", nullable = true)
    private double f2r;

    @Column(name = "f3r", nullable = true)
    private double f3r;

    @Column(name = "p1", nullable = true)
    private double p1;

    @Column(name = "p2", nullable = true)
    private double p2;

    @Column(name = "p3", nullable = true)
    private double p3;

    @ManyToOne
    @JoinColumn(name = "fornitura_id", nullable = false)
    private Fornitura fornitura;

    @ManyToMany()
    @JoinTable(
            name = "letture_fatture",
            joinColumns = @JoinColumn(name = "lettura_id"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    private List<Fattura> fatture;
}
