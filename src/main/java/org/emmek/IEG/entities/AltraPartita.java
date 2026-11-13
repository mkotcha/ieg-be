package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Importo da addebitare (o accreditare se negativo) su una fornitura nella fattura di un mese,
 * es. corrispettivo morosità o svolgimento pratica.
 * L'importo è già comprensivo di iva: non entra nell'imponibile e si somma solo al totale da pagare.
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

    // iva inclusa
    @Column(name = "importo", nullable = false)
    private Double importo;

    @Column(name = "mese", nullable = false)
    private int mese;

    @Column(name = "anno", nullable = false)
    private int anno;
}
