package org.emmek.IEG.model.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class FornituraModel {

    @ExcelCellName("POD")
    private String id;

    @ExcelCellName("id_cliente")
    private Long idCliente;

    @ExcelCellName("indirizzo fornitura")
    private String indirizzoFornitura;

    @ExcelCellName("comune fornitura")
    private String comuneFornitura;

    @ExcelCellName("provincia fornitura")
    private String provinciaFornitura;

    @ExcelCellName("cap fornitura")
    private int capFornitura;

    @ExcelCellName("potenza disponibile")
    private Double potenzaDisponibile;

    @ExcelCellName("potenza impegnata")
    private Double potenzaImpegnata;

    @ExcelCellName("tipo prelievo")
    private String tipoPrelievo;

    @ExcelCellName("tipo contatore")
    private String tipoContatore;

    @ExcelCellName("codice distributore")
    private String codiceDistributore;

    @ExcelCellName("fornitore")
    private String fornitore;

    @ExcelCellName("fatturazione")
    private String fatturazione;

    @ExcelCellName("oneri")
    private String bta;

    @ExcelCellName("iva")
    private String iva;

    @ExcelCellName("data switch")
    private String dataSwitch;

    @ExcelCellName("PUN")
    private Double maggiorazione;

    @ExcelCellName("offerta")
    private String offerta;

    @ExcelCellName("spread")
    private Double spread;


}
