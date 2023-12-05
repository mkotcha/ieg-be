package org.emmek.IEG.helpers.excel;

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
    private long idCliente;

    @ExcelCellName("indirizzo fornitura")
    private String indirizzoFornitura;

    @ExcelCellName("comune fornitura")
    private String comuneFornitura;

    @ExcelCellName("provincia fornitura")
    private String provinciaFornitura;

    @ExcelCellName("cap fornitura")
    private int capFornitura;

    @ExcelCellName("potenza disponibile")
    private double potenzaDisponibile;

    @ExcelCellName("potenza impegnata")
    private double potenzaImpegnata;

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

    @ExcelCellName("dispacciamento")
    private String dispacciamento;

    @ExcelCellName("oneri")
    private String bta;

    @ExcelCellName("iva")
    private String iva;

    @ExcelCellName("data switch")
    private String dataSwitch;

    @ExcelCellName("maggiorazione PUN")
    private double maggiorazione;

    @ExcelCellName("spread")
    private double spread;


}
