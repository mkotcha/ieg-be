package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ruoli")
@NoArgsConstructor
@AllArgsConstructor:q

public class Ruolo {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    private String ruolo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ruoli_utenti", joinColumns = @JoinColumn(name = "ruolo_id"), inverseJoinColumns = @JoinColumn(name = "utente_id"))
    @JsonIgnore
    private Set<Utente> utenti;
}
