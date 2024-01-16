package org.emmek.IEG.payloads;

public record DispacciamentoDTO(
        double capacita,
        double eolico,
        double costoAm,
        double dis,
        double int73,
        double msd,
        double sicurezza,
        double trasmissione,
        int trimestre,
        int anno

) {
}
