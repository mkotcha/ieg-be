package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pun", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mese", "anno"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Pun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
