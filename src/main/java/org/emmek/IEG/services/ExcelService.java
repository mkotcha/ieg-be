package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.emmek.IEG.entities.Fattura;
import org.emmek.IEG.entities.FatturaSingola;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
@Slf4j
public class ExcelService {

    public Workbook createWorkbook(String fileName) {
        Workbook workbook = new XSSFWorkbook();
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook.write(outputStream);
        } catch (FileNotFoundException e) {
            log.error("Unable to create excel file " + fileName + " " + e.getMessage());
        } catch (IOException e) {
            log.error("Error during writing the excel file " + fileName + " " + e.getMessage());
        }
        return workbook; // Returning workbook, but be cautious about further modifications
    }

    public Workbook createExcelFile() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Example Sheet");

        // Create a row
        Row row = sheet.createRow(0);

        // Create cells
        Cell cell = row.createCell(0);
        cell.setCellValue("Hello, World!");

        // Auto-size the column
        sheet.autoSizeColumn(0);

        return workbook;
    }

    public void saveWorkbookToFile(Workbook workbook, String filePath) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
    }

    public void addPagina2(String fileName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("python", "script/copyPagina2.py", "template/pagina_2.xlsx", fileName);
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

    public void createFattura(Fattura fattura) throws IOException {
        String numeroFattura = fattura.getNumeroFattura();
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        String fatturaFileName = "fatture/" + anno + "/" + mese + "/" + numeroFattura + ".xlsx";
        Workbook fatturaXls = createExcelFile();
        saveWorkbookToFile(fatturaXls, fatturaFileName);
        List<FatturaSingola> fattureSingole = fattura.getFattureSingole();
        for (FatturaSingola fatturaSingola : fattureSingole) {
            addPagina2(fatturaFileName);
        }
        save2Pdf(fatturaFileName);

        // close file
        fatturaXls.close();
    }
}
