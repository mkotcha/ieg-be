package org.emmek.IEG.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "forniture")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Fornitura {

//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(name = "id")
//    private UUID id;

    @Id
    @Column(name = "pod", nullable = false)
    private String id;

    @Column(name = "active")
    private boolean isActive;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "indirizzo")
    private String indirizzo;

    @Column(name = "civico")
    private String civico;

    @Column(name = "cap")
    private int cap;

    @Column(name = "comune")
    private String comune;

    @Column(name = "provincia")
    private String provincia;

    @Column(name = "potenza_disponibile", nullable = false)
    private Double potenzaDisponibile;

    @Column(name = "potenza_impegnata", nullable = false)
    private Double potenzaImpegnata;

    @Column(name = "tipo_prelievo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoPrelievo tipoPrelievo;

    @Column(name = "tipo_contatore", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoContatore tipoContatore;

    @Column(name = "codice_distributore", nullable = false)
    @Enumerated(EnumType.STRING)
    private CodiceDistributore codiceDistributore;

    @Column(name = "fornitore", nullable = true)
    private String fornitore;

    @Column(name = "fatturazione", nullable = false)
    private Fatturazione fatturazione;

    @Column(name = "bta", nullable = false)
    @Enumerated(EnumType.STRING)
    private BTA bta;

    @Column(name = "iva", nullable = false)
    private Double iva;

    @Column(name = "data_switch", nullable = false)
    private LocalDate dataSwitch;

    @Column(name = "data_switch_out")
    private LocalDate dataSwitchOut;

    @ManyToOne
    @JoinColumn(name = "offerta_id")
    private Offerta offerta;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "fatture_forniture",
            joinColumns = @JoinColumn(name = "fornitura_pod"),
            inverseJoinColumns = @JoinColumn(name = "fattura_id")
    )
    private List<Fattura> fatture;

    @JsonIgnore
    @OneToMany(mappedBy = "fornitura")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Lettura> letture;
}

