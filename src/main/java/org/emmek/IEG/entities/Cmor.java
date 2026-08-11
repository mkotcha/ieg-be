package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cmor")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Cmor {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
    private Fornitura fornitura;

    @Column(name = "importo", nullable = false)
    private Double importo;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;
}
