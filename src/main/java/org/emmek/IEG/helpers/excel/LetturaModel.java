package org.emmek.IEG.helpers.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class LetturaModel {

    @ExcelCellName("id")
    private long id;

    @ExcelCellName("POD")
    private String pod;

    @ExcelCellName("data")
    private String data;

    @ExcelCellName("tipo3")
    private String tipo = "";

    @ExcelCellName("tipo lettura")
    private String tipoLettura;

    @ExcelCellName("F1")
    private String f1;

    @ExcelCellName("F2")
    private String f2;

    @ExcelCellName("F3")
    private String f3;

    @ExcelCellName("F1r")
    private String f1r;

    @ExcelCellName("F2r")
    private String f2r;

    @ExcelCellName("F3r")
    private String f3r;

    @ExcelCellName("pF1")
    private String pF1;

    @ExcelCellName("pF2")
    private String pF2;

    @ExcelCellName("pF3")
    private String pF3;


}
