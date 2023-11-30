package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fatture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Fattura {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToMany(mappedBy = "fatture")
    private List<Fornitura> forniture;

    @ManyToOne
    @JoinColumn(name = "dispacciamento_id", nullable = false)
    private Dispacciamento dispacciamento;

    @ManyToOne
    @JoinColumn(name = "oneri_id", nullable = false)
    private Oneri oneri;

    @ManyToMany(mappedBy = "fatture")
    private List<Lettura> letture;
}
