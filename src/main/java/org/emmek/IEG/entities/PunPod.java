package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pun_pod")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PunPod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
    private Fornitura fornitura;

    @Column(name = "f1", nullable = false)
    private Double f1;

    @Column(name = "f2", nullable = false)
    private Double f2;

    @Column(name = "f3", nullable = false)
    private Double f3;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;
}
