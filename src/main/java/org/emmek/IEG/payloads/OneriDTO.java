package org.emmek.IEG.payloads;

public record OneriDTO(
        String tipo,
        Double qeTud,
        Double qpTdm,
        Double qfTud,
        Double qfMis,
        Double qeArim,
        Double qeAsos,
        Double qeUc3,
        Double qpArim,
        Double qpAsos,
        Double qpOds,
        Double qfArim,
        Double qfAsos,
        int trimestre,
        int anno

) {
}
