package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.emmek.IEG.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class ExcelService {

    @Autowired
    LetturaService letturaService;

    @Autowired
    PunService punService;


    public Workbook createExcelFile() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("sheet");
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Hello, World!");
        sheet.autoSizeColumn(0);
        return workbook;
    }

    public void saveWorkbookToFile(Workbook workbook, String filePath) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
    }

    public void addPagina(String pagina, String fileName, String sheetName) {
        String script;
        if (pagina.equals("template/pagina_1.xlsx")) {
            script = "script/copyPagina1.py";
        } else {
            script = "script/copyPagina2.py";
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", script, pagina, fileName, sheetName);
            processBuilder.redirectErrorStream(true); // Redirect error stream to stdout
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script execution failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error during Python script execution: " + e.getMessage());
        }
    }

    public void save2Pdf(String fatturaFileName) {
        String pdf = fatturaFileName.substring(0, fatturaFileName.lastIndexOf(".")) + ".pdf";
        log.debug("Avviata creazione file pdf: " + pdf);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "script/excel2Pdf.py", fatturaFileName, pdf);
            processBuilder.redirectErrorStream(true); // Redirect error stream to stdout
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script execution failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error during Python script execution: " + e.getMessage());
        }
    }

    public void writeCell(Workbook workbook, String sheetName, String cellName, String value) {
        Sheet sheet = workbook.getSheet(sheetName);
        Name namedCell = workbook.getName(cellName);
        if (namedCell == null) {
            log.error("Cell " + cellName + " not found");
            return;
        }
        CellReference cellReference = new CellReference(namedCell.getRefersToFormula());
        Row row = sheet.getRow(cellReference.getRow());
        Cell cell = row.getCell(cellReference.getCol());
        if (cell == null) {
            cell = row.createCell(cellReference.getCol());
        }
        try {
            cell.setCellValue(Double.parseDouble(value.replace(",", ".")));
        } catch (NumberFormatException ignored) {
            cell.setCellValue(value);
        }
    }

    public void writeCell(Workbook workbook, String sheetName, String cellName, double value) {
        Sheet sheet = workbook.getSheet(sheetName);
        Name namedCell = workbook.getName(cellName);
        if (namedCell == null) {
            log.error("Cell " + cellName + " not found");
            return;
        }
        CellReference cellReference = new CellReference(namedCell.getRefersToFormula());
        Row row = sheet.getRow(cellReference.getRow());
        Cell cell = row.getCell(cellReference.getCol());
        if (cell == null) {
            cell = row.createCell(cellReference.getCol());
        }
//        try {
//            cell.setCellValue(Double.toString(value).replace(",", "."));
//        } catch (NumberFormatException ignored) {
//            cell.setCellValue(value);
//        }
        cell.setCellValue(value);
    }

    public void writePagina2(String fileName, FatturaSingola fatturaSingola) {
        String sheetName = fatturaSingola.getFornitura().getId() + " letture";
        Locale it = new Locale("it", "IT");
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(fileName))) {
            writeCell(workbook, sheetName, "indirizzo_fornitura", fatturaSingola.getFornitura().getIndirizzo());
            writeCell(workbook, sheetName, "partita_iva", fatturaSingola.getFornitura().getCliente().getPIva());
            writeCell(workbook, sheetName, "codice_cliente", "2050" + String.format("%03d", fatturaSingola.getFornitura().getCliente().getId()));
            writeCell(workbook, sheetName, "tipo_contratto", fatturaSingola.getFornitura().getTipoPrelievo().toString());
            writeCell(workbook, sheetName, "pod", fatturaSingola.getFornitura().getId());
            writeCell(workbook, sheetName, "data_attivazione", fatturaSingola.getFornitura().getDataSwitch().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            writeCell(workbook, sheetName, "data_inizio_offerta", fatturaSingola.getFornitura().getDataSwitch().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            writeCell(workbook, sheetName, "tipo_misuratore", fatturaSingola.getFornitura().getTipoContatore().toString());
            writeCell(workbook, sheetName, "potenza_disponibile", String.format(it, "%.2f", fatturaSingola.getFornitura().getPotenzaDisponibile()));
            writeCell(workbook, sheetName, "potenza_impegnata", String.format(it, "%.2f", fatturaSingola.getFornitura().getPotenzaImpegnata()));
            // CONSUMO ANNUO
            writeCell(workbook, sheetName, "distributore", fatturaSingola.getFornitura().getCodiceDistributore().getLabel());
            writeCell(workbook, sheetName, "pronto_intervento", fatturaSingola.getFornitura().getCodiceDistributore().getTelefono());
            writeCell(workbook, sheetName, "data_lettura_old", fatturaSingola.getLetture().get(1).getDataLettura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            writeCell(workbook, sheetName, "data_lettura", fatturaSingola.getLetture().get(0).getDataLettura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            writeCell(workbook, sheetName, "lettura_F1_old", fatturaSingola.getLetture().get(1).getEaF1());
            writeCell(workbook, sheetName, "lettura_F1", fatturaSingola.getLetture().get(0).getEaF1());
            writeCell(workbook, sheetName, "lettura_F2_old", fatturaSingola.getLetture().get(1).getEaF2());
            writeCell(workbook, sheetName, "lettura_F2", fatturaSingola.getLetture().get(0).getEaF2());
            writeCell(workbook, sheetName, "lettura_F3_old", fatturaSingola.getLetture().get(1).getEaF3());
            writeCell(workbook, sheetName, "lettura_F3", fatturaSingola.getLetture().get(0).getEaF3());
            writeCell(workbook, sheetName, "lettura_F1r_old", fatturaSingola.getLetture().get(1).getErF1());
            writeCell(workbook, sheetName, "lettura_F1r", fatturaSingola.getLetture().get(0).getErF1());
            writeCell(workbook, sheetName, "lettura_F2r_old", fatturaSingola.getLetture().get(1).getErF2());
            writeCell(workbook, sheetName, "lettura_F2r", fatturaSingola.getLetture().get(0).getErF2());
            writeCell(workbook, sheetName, "lettura_F3r_old", fatturaSingola.getLetture().get(1).getErF3());
            writeCell(workbook, sheetName, "lettura_F3r", fatturaSingola.getLetture().get(0).getErF3());
            writeCell(workbook, sheetName, "consumo_F1", fatturaSingola.getConsumoF1());
            writeCell(workbook, sheetName, "consumo_F2", fatturaSingola.getConsumoF2());
            writeCell(workbook, sheetName, "consumo_F3", fatturaSingola.getConsumoF3());
            writeCell(workbook, sheetName, "consumo_F1r", fatturaSingola.getConsumoF1r());
            writeCell(workbook, sheetName, "consumo_F2r", fatturaSingola.getConsumoF2r());
            writeCell(workbook, sheetName, "consumo_F3r", fatturaSingola.getConsumoF3r());
            writeCell(workbook, sheetName, "consumo_tot", fatturaSingola.getConsumoTot());
            writeCell(workbook, sheetName, "potenza_prelevata", fatturaSingola.getPotenzaPrelevata());
            writeCell(workbook, sheetName, "periodo_imposte", LocalDate.of(fatturaSingola.getFattura().getAnno(), fatturaSingola.getFattura().getMese(), 15).format(DateTimeFormatter.ofPattern("MMMM yy", Locale.ITALIAN)));
            if (fatturaSingola.getLetture().get(0).getTipoLettura().toString().equals("STIMATO")) {
                writeCell(workbook, sheetName, "tipo_consumo", "Consumi stimati");
            } else {
                writeCell(workbook, sheetName, "tipo_consumo", "Consumi effettivi");
            }
            Period period = Period.between(fatturaSingola.getFattura().getDataFattura(), LocalDate.now());
            int delta = period.getMonths() + 1;
            double totF1 = 0;
            double totF2 = 0;
            double totF3 = 0;
            for (int i = 0; i < 12; i++) {
                Map<String, Double> consumi = letturaService.getConsumi(fatturaSingola.getFornitura(), i + delta);
                writeCell(workbook, sheetName, "consumi_" + i + "_f1", String.format(it, "%.2f", consumi.get("EaF1")));
                writeCell(workbook, sheetName, "consumi_" + i + "_f2", String.format(it, "%.2f", consumi.get("EaF2")));
                writeCell(workbook, sheetName, "consumi_" + i + "_f3", String.format(it, "%.2f", consumi.get("EaF3")));
                totF1 += consumi.get("EaF1");
                totF2 += consumi.get("EaF2");
                totF3 += consumi.get("EaF3");
            }
            writeCell(workbook, sheetName, "consumo_annuo_f1", totF1);
            writeCell(workbook, sheetName, "consumo_annuo_f2", totF2);
            writeCell(workbook, sheetName, "consumo_annuo_f3", totF3);
            writeCell(workbook, sheetName, "consumo_annuo", totF1 + totF2 + totF3);
            for (int i = 0; i < 12; i++) {
                String meseStr = fatturaSingola.getFattura().getDataFattura().minusMonths(i + delta + 1).format(DateTimeFormatter.ofPattern("MMMM", it));
                writeCell(workbook, sheetName, "mese_grafico_" + i, meseStr);
            }

            saveWorkbookToFile(workbook, fileName);
        } catch (IOException e) {
            log.error("Error during workbook creation: " + e.getMessage());
        }
    }

    private void writePagina3(String fileName, FatturaSingola fatturaSingola) {
        String sheetName = fatturaSingola.getFornitura().getId() + " consumi";
        Locale it = new Locale("it", "IT");
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(fileName))) {
            writeCell(workbook, sheetName, "codice_cliente", "2050" + String.format("%03d", fatturaSingola.getFornitura().getCliente().getId()));
            writeCell(workbook, sheetName, "tipo_misuratore", fatturaSingola.getFornitura().getTipoContatore().toString());
            writeCell(workbook, sheetName, "pod", fatturaSingola.getFornitura().getId());
            writeCell(workbook, sheetName, "potenza_disponibile", String.format(it, "%.2f", fatturaSingola.getFornitura().getPotenzaDisponibile()));
            writeCell(workbook, sheetName, "potenza_impegnata", String.format(it, "%.2f", fatturaSingola.getFornitura().getPotenzaImpegnata()));
            writeCell(workbook, sheetName, "data_lettura_prec", fatturaSingola.getLetture().get(1).getDataLettura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            writeCell(workbook, sheetName, "data_lettura", fatturaSingola.getLetture().get(0).getDataLettura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            Pun pun = punService.getByMeseAndAnno(fatturaSingola.getFattura().getMese(), fatturaSingola.getFattura().getAnno());
            writeCell(workbook, sheetName, "prezzo_F1", pun.getF1());
            writeCell(workbook, sheetName, "prezzo_F2", pun.getF2());
            writeCell(workbook, sheetName, "prezzo_F3", pun.getF3());
            writeCell(workbook, sheetName, "consumo_F1", fatturaSingola.getConsumoF1());
            writeCell(workbook, sheetName, "consumo_F2", fatturaSingola.getConsumoF2());
            writeCell(workbook, sheetName, "consumo_F3", fatturaSingola.getConsumoF3());
            writeCell(workbook, sheetName, "perdite_F1", fatturaSingola.getPerditeF1());
            writeCell(workbook, sheetName, "perdite_F2", fatturaSingola.getPerditeF2());
            writeCell(workbook, sheetName, "perdite_F3", fatturaSingola.getPerditeF3());
            writeCell(workbook, sheetName, "consumo_tot", fatturaSingola.getConsumoTot());
            writeCell(workbook, sheetName, "consumo_tot_perdite", fatturaSingola.getConsumoTotP());
            writeCell(workbook, sheetName, "spread", fatturaSingola.getFornitura().getPrezzo().getSpread());
            writeCell(workbook, sheetName, "commercializzazione", fatturaSingola.getFornitura().getProgrammazione().getCommercializzazione());
            Dispacciamento dispacciamento = fatturaSingola.getDispacciamento();
            writeCell(workbook, sheetName, "costo_am", dispacciamento.getCostoAm());
            writeCell(workbook, sheetName, "MSD", dispacciamento.getMsd());
            writeCell(workbook, sheetName, "sicurezza", dispacciamento.getSicurezza());
            writeCell(workbook, sheetName, "eolico", dispacciamento.getEolico());
            writeCell(workbook, sheetName, "DIS", dispacciamento.getDis());
            writeCell(workbook, sheetName, "capacita", dispacciamento.getCapacita());
            double parzialeMateria = fatturaSingola.getConsumoF1() * pun.getF1() +
                    fatturaSingola.getConsumoF2() * pun.getF2() +
                    fatturaSingola.getConsumoF3() * pun.getF3() +
                    fatturaSingola.getPerditeF1() * pun.getF1() +
                    fatturaSingola.getPerditeF2() * pun.getF2() +
                    fatturaSingola.getPerditeF3() * pun.getF3() +
                    fatturaSingola.getConsumoTotP() * fatturaSingola.getFornitura().getPrezzo().getSpread();
            writeCell(workbook, sheetName, "formula_programmazione", fatturaSingola.getFornitura().getProgrammazione().getOneriProgrammazione() * parzialeMateria / 100);
            writeCell(workbook, sheetName, "INT", dispacciamento.getInt73());
            writeCell(workbook, sheetName, "totale_materia", fatturaSingola.getTotaleMateria());
            Oneri oneri = fatturaSingola.getOneri();
            writeCell(workbook, sheetName, "qf_tud", oneri.getQfTud());
            writeCell(workbook, sheetName, "qf_mis", oneri.getQfMis());
            writeCell(workbook, sheetName, "qp_tdm", oneri.getQpTdm());
            writeCell(workbook, sheetName, "qe_tud", oneri.getQeTud());
            writeCell(workbook, sheetName, "qe_uc3", oneri.getQeUc3());
            writeCell(workbook, sheetName, "trasmissione", dispacciamento.getTrasmissione());
            writeCell(workbook, sheetName, "totale_trasporto", fatturaSingola.getTotaleTrasporto());
            writeCell(workbook, sheetName, "qf_asos", oneri.getQfAsos());
            writeCell(workbook, sheetName, "qf_arim", oneri.getQfArim());
            writeCell(workbook, sheetName, "qp_asos", oneri.getQpAsos());
            writeCell(workbook, sheetName, "qp_arim", oneri.getQpArim());
            writeCell(workbook, sheetName, "qe_asos", oneri.getQeAsos());
            writeCell(workbook, sheetName, "qe_arim", oneri.getQeArim());
            writeCell(workbook, sheetName, "totale_oneri", fatturaSingola.getTotaleOneri());
            writeCell(workbook, sheetName, "totale_imposte", fatturaSingola.getTotaleImposte());
            saveWorkbookToFile(workbook, fileName);
        } catch (IOException e) {
            log.error("Error during workbook creation: " + e.getMessage());
        }
    }

    public void writePagina1(String fileName, FatturaSingola fatturaSingola) {
        String sheetName = "riepilogo";
        Locale it = new Locale("it", "IT");
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(fileName))) {


        } catch (IOException e) {
            log.error("Error during workbook creation: " + e.getMessage());
        }
    }


    public void createFattura(Fattura fattura) throws IOException {
        String numeroFattura = fattura.getNumeroFattura();
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        String fatturaFileName = "fatture/" + anno + "/" + mese + "/" + numeroFattura + ".xlsx";
        log.debug("Avviata creazione file excel: " + fatturaFileName);
        Workbook fatturaXls = createExcelFile();
        saveWorkbookToFile(fatturaXls, fatturaFileName);
        fatturaXls.close();
        List<FatturaSingola> fattureSingole = fattura.getFattureSingole();
        for (FatturaSingola fatturaSingola : fattureSingole) {
            addPagina("template/pagina_2.xlsx", fatturaFileName, fatturaSingola.getFornitura().getId() + " letture");
            writePagina2(fatturaFileName, fatturaSingola);
            addPagina("template/pagina_3.xlsx", fatturaFileName, fatturaSingola.getFornitura().getId() + " consumi");
            writePagina3(fatturaFileName, fatturaSingola);
        }
        addPagina("template/pagina_1.xlsx", fatturaFileName, "");
        writePagina1(fatturaFileName, fattura);

        save2Pdf(fatturaFileName);

        // close file

    }


}
