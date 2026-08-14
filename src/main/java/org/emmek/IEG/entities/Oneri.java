package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.BTA;

@Entity
@Table(name = "oneri")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Oneri {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private BTA tipo;

    @Column(name = "qe_tud", nullable = false)
    private Double qeTud;

    @Column(name = "qp_tdm", nullable = false)
    private Double qpTdm;

    @Column(name = "qf_tud", nullable = false)
    private Double qfTud;

    @Column(name = "qf_mis", nullable = false)
    private Double qfMis;

    @Column(name = "qe_arim", nullable = false)
    private Double qeArim;

    @Column(name = "qe_asos", nullable = false)
    private Double qeAsos;

    @Column(name = "qe_uc3", nullable = false)
    private Double qeUc3;

    @Column(name = "qe_uc6", nullable = false)
    private Double qeUc6;

    @Column(name = "qp_arim", nullable = false)
    private Double qpArim;

    @Column(name = "qp_asos", nullable = false)
    private Double qpAsos;

    @Column(name = "qf_arim", nullable = false)
    private Double qfArim;

    @Column(name = "qf_asos", nullable = false)
    private Double qfAsos;

    @Column(name = "qf_uc6", nullable = false)
    private Double qfUc6;

    @Column(name = "qf_dbt", nullable = false)
    private Double qfDbt;

    @Column(name = "trasmissione", nullable = false)
    private Double trasmissione;

    @Column(name = "accise", nullable = false)
    private Double accise;

    @Column(name = "trimestre", nullable = false)
    private int trimestre;

    @Column(name = "anno", nullable = false)
    private int anno;

}
