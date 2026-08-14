package org.emmek.IEG.payloads;

public record DispacciamentoDTO(
        Double capacita,
        Double costoAm,
        Double dis,
        Double sbilanciamento,
        int mese,
        int anno

) {
}
