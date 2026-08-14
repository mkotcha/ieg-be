package org.emmek.IEG.model.excel;


import com.poiji.annotation.ExcelCellName;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class OffertaModel {
    @ExcelCellName("id")
    private Long id;

    @ExcelCellName("tipo")
    private String nome;

    @ExcelCellName("codice")
    private String codice;

    @ExcelCellName("nome")
    private String tipo;

    @ExcelCellName("capacita plus")
    private boolean isCapacitaMaggiorazione;

    @ExcelCellName("capacita")
    private String maggiorazione;

    @ExcelCellName("oneri_programmazione")
    private String ocv;

    @ExcelCellName("programmazione")
    private String descrizione;

    @ExcelCellName("commercializzazione")
    private String ocf;

    @ExcelCellName("data_inizio")
    private String dataInizio;

    @ExcelCellName("data_fine")
    private String dataFine;


}
