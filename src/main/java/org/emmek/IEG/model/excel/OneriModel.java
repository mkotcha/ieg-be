package org.emmek.IEG.model.excel;

import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class OneriModel {
    @ExcelCellName("id")
    private Long id;

    @ExcelCellName("tipo")
    private String tipo;

    @ExcelCellName("qe_tud")
    private Double qeTud;

    @ExcelCellName("qp_tdm")
    private Double qpTdm;

    @ExcelCellName("qf_tud")
    private Double qfTud;

    @ExcelCellName("qf_mis")
    private Double qfMis;

    @ExcelCellName("qe_arim")
    private Double qeArim;

    @ExcelCellName("qe_asos")
    private Double qeAsos;

    @ExcelCellName("qe_uc3")
    private Double qeUc3;

    @ExcelCellName("qe_uc6")
    private Double qeUc6;

    @ExcelCellName("qp_arim")
    private Double qpArim;

    @ExcelCellName("qp_asos")
    private Double qpAsos;

    @ExcelCellName("qf_arim")
    private Double qfArim;

    @ExcelCellName("qf_asos")
    private Double qfAsos;

    @ExcelCellName("qf_uc6")
    private Double qfUc6;

    @ExcelCellName("qf_dbt")
    private Double qfDbt;

    @ExcelCellName("trasmissione")
    private Double trasmissione;

    @ExcelCellName("accise")
    private Double accise;

    @ExcelCellName("trimestre")
    private Integer trimestre;

    @ExcelCellName("anno")
    private Integer anno;
}
