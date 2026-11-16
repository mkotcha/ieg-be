package org.emmek.IEG.payloads;

import jakarta.validation.constraints.NotBlank;
import org.emmek.IEG.enums.ModalitaProgrammazione;
import org.emmek.IEG.enums.TipoOfferta;
import org.emmek.IEG.enums.ValueOfEnum;

import java.time.LocalDate;

public record OffertaDTO(
        @NotBlank(message = "nome obbligatorio")
        String nome,
        String codice,
        @ValueOfEnum(enumClass = TipoOfferta.class, message = "tipo deve essere PUN, FISSO o NEW")
        String tipo,
        Double f1,
        Double f2,
        Double f3,
        boolean pun,
        boolean capacitaMaggiorazione,
        Double maggiorazione,
        boolean maggiorazioneSbilanciamento,
        boolean hasSpread,
        Double spread,
        Double ocv,
        // OCV (default) o CLASSIC
        @ValueOfEnum(enumClass = ModalitaProgrammazione.class, message = "modalitaProgrammazione deve essere OCV o CLASSIC")
        String modalitaProgrammazione,
        Double ocf,
        String descrizione,
        LocalDate dataInizio,
        LocalDate dataFine
) {
}
