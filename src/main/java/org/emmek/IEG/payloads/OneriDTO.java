package org.emmek.IEG.payloads;

public record OneriDTO(
        String tipo,
        double qeTud,
        double qpTdm,
        double qfTud,
        double qfMis,
        double qeArim,
        double qeAsos,
        double qeUc3,
        double qpArim,
        double qpAsos,
        double qpOds,
        double qfArim,
        double qfAsos,
        int trimestre,
        int anno

) {
}
