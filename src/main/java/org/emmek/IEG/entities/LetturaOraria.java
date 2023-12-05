package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Entity
@Table(name = "letture_orarie")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LetturaOraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String giorno;

    @ElementCollection
    private Map<String, String> attributi;

    @ManyToOne
    @JoinColumn(name = "lettura_id")
    private Lettura lettura;
}
