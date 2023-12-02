package org.emmek.IEG.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clienti")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Cliente {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "ragione_sociale", nullable = false)
    private String ragioneSociale;

    @Column(name = "piva", nullable = false)
    private String pIva;

    @Column(name = "CF", nullable = false)
    private String cf;

    @Column(name = "indirizzo", nullable = false)
    private String indirizzo;

    @Column(name = "cap", nullable = false)
    private int cap;

    @Column(name = "comune", nullable = false)
    private String comune;

    @Column(name = "provincia", nullable = false)
    private String provincia;

    @Column(name = "telefono", nullable = true)
    private String telefono;

    @Column(name = "email", nullable = true)
    private String email;
}
