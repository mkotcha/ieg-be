package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.entities.FatturaSingola;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ExcelService {

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
                System.out.println(line);
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
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "script/excel2Pdf.py", fatturaFileName, pdf);
            processBuilder.redirectErrorStream(true); // Redirect error stream to stdout
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
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
        CellReference cellReference = new CellReference(namedCell.getRefersToFormula());
        Row row = sheet.getRow(cellReference.getRow());
        Cell cell = row.getCell(cellReference.getCol());
        if (cell == null) {
            cell = row.createCell(cellReference.getCol());
        }
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
            saveWorkbookToFile(workbook, fileName);
        } catch (IOException e) {
            log.error("Error during workbook creation: " + e.getMessage());
        }
    }

    public void createFattura(Fattura fattura) throws IOException {
        String numeroFattura = fattura.getNumeroFattura();
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        String fatturaFileName = "fatture/" + anno + "/" + mese + "/" + numeroFattura + ".xlsx";
        Workbook fatturaXls = createExcelFile();
        saveWorkbookToFile(fatturaXls, fatturaFileName);
        fatturaXls.close();
        List<FatturaSingola> fattureSingole = fattura.getFattureSingole();
        for (FatturaSingola fatturaSingola : fattureSingole) {
            addPagina("template/pagina_2.xlsx", fatturaFileName, fatturaSingola.getFornitura().getId() + " letture");
            writePagina2(fatturaFileName, fatturaSingola);
            addPagina("template/pagina_3.xlsx", fatturaFileName, fatturaSingola.getFornitura().getId() + " consumi");
        }
        addPagina("template/pagina_1.xlsx", fatturaFileName, "");

        save2Pdf(fatturaFileName);

        // close file

    }
}
