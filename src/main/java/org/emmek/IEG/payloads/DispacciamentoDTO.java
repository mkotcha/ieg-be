package org.emmek.IEG.payloads;

public record DispacciamentoDTO(
        Double capacita,
        Double eolico,
        Double costoAm,
        Double dis,
        Double int73,
        Double msd,
        Double sicurezza,
        Double trasmissione,
        int trimestre,
        int anno

) {
}
