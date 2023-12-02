package org.emmek.IEG.helpers;

import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ClienteModel {

    @ExcelCellName("id")
    private long id;

    @ExcelCellName("ragione sociale")
    private String ragioneSociale;

    @ExcelCellName("p iva")
    private String pIva;

    @ExcelCellName("CF")
    private String cf;

    @ExcelCellName("telefono")
    private String telefono;

    @ExcelCellName("email")
    private String email;

    @ExcelCellName("indirizzo")
    private String indirizzo;

    @ExcelCellName("cap")
    private int cap;

    @ExcelCellName("comune")
    private String comune;

    @ExcelCellName("provincia")
    private String provincia;
}

