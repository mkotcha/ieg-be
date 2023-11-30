package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.BTA;

import java.util.UUID;

@Entity
@Table(name = "oneri")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Oneri {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private BTA tipo;

    @Column(name = "qe_tud", nullable = false)
    private double qeTud;

    @Column(name = "qp_tdm", nullable = false)
    private double qpTdm;

    @Column(name = "qf_tud", nullable = false)
    private double qfTud;

    @Column(name = "qf_mis", nullable = false)
    private double qfMis;

    @Column(name = "qe_arim", nullable = false)
    private double qeArim;

    @Column(name = "qe_asos", nullable = false)
    private double qeAsos;

    @Column(name = "qe_uc3", nullable = false)
    private double qeUc3;

    @Column(name = "qp_arim", nullable = false)
    private double qpArim;

    @Column(name = "qp_asos", nullable = false)
    private double qpAsos;

    @Column(name = "qp_ods", nullable = false)
    private double qpOds;

    @Column(name = "qf_arim", nullable = false)
    private double qfArim;

    @Column(name = "qf_asos", nullable = false)
    private double qfAsos;

    @Column(name = "trimestre", nullable = false)
    private int trimestre;

    @Column(name = "anno", nullable = false)
    private int anno;
    
}
