package org.emmek.IEG.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.emmek.IEG.enums.VoceConfronto;

/**
 * Regola per ricondurre una riga del fornitore a una voce di confronto.
 * chiave: cod tariffa della riga (o nr articolo se la tariffa è vuota), esatta o prefisso se finisce con '*'.
 * perdite: la regola vale per le righe "Perdite ..." (stessa tariffa dell'energia).
 * quantita: la riga concorre alla quantità della voce (es. del dispacciamento conta solo una componente, perché
 * tutte riportano gli stessi kWh).
 */
@Entity
@Table(name = "mappature_voci_fornitore")
@NoArgsConstructor
@Getter
@Setter
public class MappaturaVoceFornitore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String chiave;

    @Column(nullable = false)
    private boolean perdite;

    @Column(name = "voce_confronto", nullable = false)
    @Enumerated(EnumType.STRING)
    private VoceConfronto voceConfronto;

    @Column(nullable = false)
    private boolean quantita;

    public boolean isPrefisso() {
        return chiave.endsWith("*");
    }

    public boolean corrisponde(String valore) {
        if (valore == null || valore.isBlank()) return false;
        String v = valore.trim();
        return isPrefisso() ? v.startsWith(chiave.substring(0, chiave.length() - 1)) : v.equals(chiave);
    }
}
