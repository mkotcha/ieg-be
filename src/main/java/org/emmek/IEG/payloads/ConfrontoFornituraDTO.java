package org.emmek.IEG.payloads;

import java.util.List;

/**
 * Confronto di una fornitura (fattura singola) con le righe del fornitore dello stesso POD e mese di competenza,
 * da qualunque bolletta arrivino (i conguagli si sommano al mese a cui si riferiscono).
 *
 * @param totaleNostro     somma delle nostre voci (imponibile, senza iva)
 * @param totaleFornitore  somma delle righe del fornitore (esclusi i costi IEG)
 * @param margine          nostro - fornitore sulle voci che concorrono al margine (escluse altre partite)
 * @param bolletteFornitore bollette del fornitore da cui arrivano le righe
 * @param conguagli        tra le righe del fornitore ci sono ricalcoli (storni e rifatturazioni)
 */
public record ConfrontoFornituraDTO(
        String pod,
        String bta,
        String btaFornitore,
        Double potenzaImpegnata,
        Double potenzaImpegnataFornitore,
        List<String> bolletteFornitore,
        boolean conguagli,
        double totaleNostro,
        double totaleFornitore,
        double margine,
        double differenzaPassanti,
        List<String> anomalie,
        List<ConfrontoVoceDTO> voci
) {
}
