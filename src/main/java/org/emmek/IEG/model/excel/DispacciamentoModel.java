package org.emmek.IEG.model.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class DispacciamentoModel {
    @ExcelCellName("id")
    private Long id;

    @ExcelCellName("costo_am")
    private Double costoAm;

    @ExcelCellName("DIS")
    private Double dis;

    @ExcelCellName("capacita")
    private Double capacita;

    @ExcelCellName("sbilanciamento")
    private Double sbilanciamento;

    @ExcelCellName("mese")
    private Integer mese;

    @ExcelCellName("anno")
    private Integer anno;
}
