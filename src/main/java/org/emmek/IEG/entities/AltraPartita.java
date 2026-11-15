package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Importo da addebitare (o accreditare se negativo) su una fornitura nella fattura di un mese,
 * es. corrispettivo morosità, svolgimento pratica, pratica di aumento di potenza.
 * Se esente iva (importo già comprensivo di iva o fuori campo, es. morosità) non entra nell'imponibile e si somma
 * solo al totale da pagare; altrimenti l'importo è imponibile e paga l'iva della fornitura.
 */
@Entity
@Table(name = "altre_partite")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AltraPartita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fornitura_pod", nullable = false)
    private Fornitura fornitura;

    @Column(name = "voce", nullable = false)
    private String voce;

    @Column(name = "importo", nullable = false)
    private Double importo;

    @Column(name = "esente_iva", nullable = false)
    private boolean esenteIva = true;

    // false = esclusa dalle fatture ma conservata, si può riattivare
    @Column(name = "attiva", nullable = false)
    private boolean attiva = true;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;
}
