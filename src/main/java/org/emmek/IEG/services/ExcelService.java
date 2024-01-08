package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
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
        Workbook workbook = null;
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            workbook = new XSSFWorkbook();
            workbook.write(outputStream);
        } catch (FileNotFoundException e) {
            log.error("Impossibile creare file excel " + fileName + " " + e.getMessage());
        } catch (IOException e) {
            log.error("errore durante la scrittura del file excel " + fileName + " " + e.getMessage());
        }
        return workbook;
    }


    public void addPagina2(String fileName) {
        String pagina2 = "template/pagina_2.xlsx";

        ProcessBuilder processBuilder = new ProcessBuilder("python", "script/copyPagina2.py", pagina2, fileName);
        processBuilder.redirectErrorStream(true); // Redirects error output to the standard output
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
            }
            int exitCode = process.waitFor();
            log.debug("Python script executed, exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createFattura(Fattura fattura) throws IOException {
        String numeroFattura = fattura.getNumeroFattura();
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        String fatturaFileName = "fatture/" + anno + "/" + mese + "/" + numeroFattura + ".xlsx";
        Workbook fatturaXls = createWorkbook(fatturaFileName);
        List<FatturaSingola> fattureSingole = fattura.getFattureSingole();
        for (FatturaSingola fatturaSingola : fattureSingole) {
            addPagina2(fatturaFileName);
        }
        // Write the output to a file
//        FileOutputStream fileOut = new FileOutputStream(fatturaFileName);
//        fatturaXls.write(fileOut);

    }
}
