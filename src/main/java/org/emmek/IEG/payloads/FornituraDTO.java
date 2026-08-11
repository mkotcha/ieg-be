package org.emmek.IEG.payloads;

public record FornituraDTO(
        String id,
        String bta,
        String cap,
        String idCliente,
        String codiceDistributore,
        String comune,
        String dataSwitch,
        String dataSwitchOut,
        String fatturazione,
        String fornitore,
        String indirizzo,
        String iva,
        String potenzaDisponibile,
        String potenzaImpegnata,
        String idPrezzo,
        String provincia,
        String tipoContatore,
        String tipoPrelievo
) {
}
