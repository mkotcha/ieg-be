package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "clienti")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Cliente {
    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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

    @JsonIgnore
    @OneToMany(mappedBy = "cliente")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<Fornitura> forniture;
}
