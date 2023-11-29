package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "indirizzi")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Indirizzo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "indirizzo", nullable = false)
    private String indirizzo;

    @Column(name = "cap", nullable = false)
    private int cap;

    @Column(name = "comune", nullable = false)
    private String comune;

    @Column(name = "provincia", nullable = false)
    private String provincia;

}
