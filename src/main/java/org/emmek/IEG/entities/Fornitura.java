package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "forniture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Fornitura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "pod", nullable = false)
    private String pod;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "indirizzo_id", nullable = false)
    private Indirizzo indirizzo;

    @Column(name = "potenza_disponibile", nullable = false)
    private double potenzaDisponibile;

    @Column(name = "potenza_impegnata", nullable = false)
    private double potenzaImpegnata;

    @Column(name = "tipo_prelievo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoPrelievo tipoPrelievo;

    @Column(name = "tipo_contatore", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoContatore;

    @Column(name = "codice_distributore", nullable = false)
    @Enumerated(EnumType.STRING)
    private CodiceDistributore codiceDistributore;

    @Column(name = "fornitore", nullable = false)
    private String fornitore;

    @Column(name = "fatturazione", nullable = false)
    private Fatturazione fatturazione;

    @Column(name = "bta", nullable = false)
    @Enumerated(EnumType.STRING)
    private BTA bta;

    @Column(name = "iva", nullable = false)
    private double iva;

    @Column(name = "data_switch", nullable = false)
    private LocalDate dataSwitch;

    @Column(name = "data_switch_out")
    private LocalDate dataSwitchOut;

    @ManyToOne
    @JoinColumn(name = "prezzo_id", nullable = false)
    private Prezzo prezzo;

    @ManyToOne
    @JoinColumn(name = "programmazione_id", nullable = false)
    private Programmazione programmazione;

    @ManyToMany
    @JoinTable(
            name = "fatture_forniture",
            joinColumns = @JoinColumn(name = "fornitura_id"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    private List<Fattura> fatture;
}

