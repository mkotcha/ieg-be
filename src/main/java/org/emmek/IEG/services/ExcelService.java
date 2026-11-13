package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.SezioneFattura;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Genera la fattura excel/pdf a partire dai template in template/.
 * I template non contengono formule: tutti i valori sono calcolati in Java (CalcoloFatturaService),
 * salvati su fattura/fattura singola e qui scritti nelle celle con nome.
 */
@Service
@Slf4j
public class ExcelService {

    private static final Locale IT = Locale.ITALIAN;
    private static final String EURO = "€";

    // colonne delle righe di dettaglio nel template pagina_3 (la riga è data dalla cella con nome voce_[codice])
    private static final int COL_DESCRIZIONE = 0;   // A
    private static final int COL_DAL = 6;           // G
    private static final int COL_AL = 7;            // H
    private static final int COL_UNITA = 8;         // I
    private static final int COL_UNITARIO = 9;      // J
    private static final int COL_QUANTITA = 10;     // K
    private static final int COL_IMPORTO = 11;      // L
    private static final int COL_IVA = 12;          // M

    @Autowired
    FatturaRepository fatturaRepository;

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
        File parent = new File(filePath).getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
    }

    public void addPagina(String pagina, String fileName, String sheetName) {
        String script = pagina.equals("template/pagina_1.xlsx") ? "script/copyPagina1.py" : "script/copyPagina2.py";
        eseguiScript(script, pagina, fileName, sheetName);
    }

    public void save2Pdf(String fatturaFileName) {
        String pdf = fatturaFileName.substring(0, fatturaFileName.lastIndexOf(".")) + ".pdf";
        log.debug("Avviata creazione file pdf: " + pdf);
        eseguiScript("script/excel2Pdf.py", fatturaFileName, pdf);
    }

    /**
     * Esegue uno script python che pilota Excel. Se fallisce interrompe la generazione della fattura,
     * invece di produrre un file incompleto.
     */
    private void eseguiScript(String script, String... args) {
        List<String> comando = new java.util.ArrayList<>(List.of("python", script));
        comando.addAll(List.of(args));
        StringBuilder output = new StringBuilder();
        int exitCode;
        try {
            Process process = new ProcessBuilder(comando).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug(line);
                    output.append(line).append('\n');
                }
            }
            exitCode = process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("Impossibile eseguire " + script + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Esecuzione di " + script + " interrotta", e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("Script " + script + " fallito (exit code " + exitCode + ") con argomenti "
                    + String.join(" | ", args) + "\n" + output);
        }
    }

    // ------------------------------------------------------------------
    //  accesso alle celle con nome
    // ------------------------------------------------------------------

    /**
     * Cerca la cella con nome nel foglio indicato. Gli stessi nomi sono presenti su più fogli
     * (es. iva, pod, codice_cliente), quindi si usa solo il nome che punta a quel foglio.
     */
    private Cell findCell(Workbook workbook, String sheetName, String cellName) {
        return findCell(workbook, sheetName, cellName, -1);
    }

    /**
     * Come findCell, ma con colonna diversa da quella della cella con nome (colonna >= 0).
     */
    private Cell findCell(Workbook workbook, String sheetName, String cellName, int colonna) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            log.error("Foglio " + sheetName + " non trovato");
            return null;
        }
        for (Name name : workbook.getNames(cellName)) {
            String refSheet;
            try {
                refSheet = name.getSheetName();
            } catch (RuntimeException e) {
                continue; // nome con riferimento non valido (#REF! o file esterno)
            }
            if (!sheetName.equals(refSheet)) continue;
            CellReference ref = new CellReference(name.getRefersToFormula());
            int col = colonna >= 0 ? colonna : ref.getCol();
            Row row = sheet.getRow(ref.getRow());
            if (row == null) row = sheet.createRow(ref.getRow());
            Cell cell = row.getCell(col);
            if (cell == null) cell = row.createCell(col);
            return cell;
        }
        log.error("Cella " + cellName + " non trovata nel foglio " + sheetName);
        return null;
    }

    private static void setValue(Cell cell, Object value) {
        if (value == null) cell.setBlank();
        else if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        else if (value instanceof LocalDate d) cell.setCellValue(d);
        else cell.setCellValue(value.toString());
    }

    public void writeCell(Workbook workbook, String sheetName, String cellName, Object value) {
        Cell cell = findCell(workbook, sheetName, cellName);
        if (cell == null) return;
        if (value == null) log.warn("Valore nullo per la cella " + cellName + " nel foglio " + sheetName);
        setValue(cell, value);
    }

    // ------------------------------------------------------------------
    //  pagine 2 e 3: una coppia per ogni fornitura
    // ------------------------------------------------------------------

    /**
     * Compila il dettaglio (pagina 3) e lo scontrino (pagina 2) di una fornitura.
     * I due fogli devono essere già stati aggiunti al file.
     */
    private void writeFornitura(String fileName, FatturaSingola fs) throws IOException {
        Fornitura fornitura = fs.getFornitura();
        Fattura fattura = fs.getFattura();
        Offerta offerta = fornitura.getOfferta();
        Lettura lettura = fs.getLetturaFinale();
        Lettura letturaOld = fs.getLetturaIniziale();
        double kp = lettura.getKp() != null ? lettura.getKp() : 1;

        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(fileName))) {
            // ---------------- pagina 3: dettaglio ----------------
            String s = fornitura.getId() + " consumi";
            writeCell(workbook, s, "codice_cliente", codiceCliente(fornitura.getCliente()));
            writeCell(workbook, s, "pod", fornitura.getId());
            writeCell(workbook, s, "potenza_disponibile", fornitura.getPotenzaDisponibile());
            writeCell(workbook, s, "tipo_misuratore", capitalize(fornitura.getTipoContatore().name()));
            writeCell(workbook, s, "potenza_impegnata", fs.getPotenzaOneri());
            for (VoceFatturaSingola voce : fs.getVoci()) {
                if (voce.getSezione() == SezioneFattura.ALTRE_PARTITE) continue; // riga unica, scritta sotto
                String nome = "voce_" + voce.getCodice();
                if (findCell(workbook, s, nome) == null) continue;
                setValue(findCell(workbook, s, nome, COL_DESCRIZIONE), voce.getDescrizione());
                setValue(findCell(workbook, s, nome, COL_DAL), voce.getDal());
                setValue(findCell(workbook, s, nome, COL_AL), voce.getAl());
                setValue(findCell(workbook, s, nome, COL_UNITA), voce.getUnita());
                setValue(findCell(workbook, s, nome, COL_UNITARIO), voce.getUnitario());
                setValue(findCell(workbook, s, nome, COL_QUANTITA), voce.getQuantita());
                setValue(findCell(workbook, s, nome, COL_IMPORTO), voce.getImporto());
                setValue(findCell(workbook, s, nome, COL_IVA), voce.getIva());
            }
            writeCell(workbook, s, "totale_materia", fs.getTotaleMateria());
            writeCell(workbook, s, "totale_trasporto", fs.getTotaleTrasporto());
            writeCell(workbook, s, "totale_oneri", fs.getTotaleOneri());
            writeCell(workbook, s, "totale_accise", fs.getTotaleImposte());
            writeCell(workbook, s, "altre_partite_1", altrePartite(fs));
            writeCell(workbook, s, "totale_partite", fs.getTotalePartite());
            writeCell(workbook, s, "aliquota_iva", fornitura.getIva() / 100);
            writeCell(workbook, s, "imponibile", fs.getTotaleImponibile());
            writeCell(workbook, s, "totale_iva", fs.getTotaleIva());

            // ---------------- pagina 2: scontrino ----------------
            s = fornitura.getId() + " letture";
            double consumo = nvl(fs.getConsumoTot());
            double spesaFissa = nvl(fs.getSpesaFissaVendita()) + nvl(fs.getSpesaFissaRete());
            // imponibile + iva: comprende anche le voci che non confluiscono nelle righe dello scontrino
            double totaleBolletta = nvl(fs.getTotaleImponibile()) + nvl(fs.getTotaleIva());
            writeCell(workbook, s, "pod", fornitura.getId());
            writeCell(workbook, s, "indirizzo_fornitura", fornitura.getIndirizzo() + ", " + fornitura.getCap() + " "
                    + fornitura.getComune() + " (" + fornitura.getProvincia() + ")");
            writeCell(workbook, s, "potenza_impegnata", numero(fornitura.getPotenzaImpegnata()) + " kW");
            // quota per consumi
            writeCell(workbook, s, "consumo_netto", formato(consumo, "#,##0.000") + " kWh");
            writeCell(workbook, s, "spesa_consumi", nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaRete()));
            writeCell(workbook, s, "spesa_media", prezzoMedio(fs.getSpesaMedia(), "/kWh"));
            writeCell(workbook, s, "spesa_vendita", fs.getSpesaVendita());
            writeCell(workbook, s, "spesa_vendita_media", prezzoMedio(fs.getSpesaVenditaMedia(), "/kWh"));
            writeCell(workbook, s, "spesa_rete", fs.getSpesaRete());
            writeCell(workbook, s, "spesa_rete_media", prezzoMedio(fs.getSpesaReteMedia(), "/kWh"));
            // quota fissa (1 mese)
            writeCell(workbook, s, "spesa_fissa", spesaFissa);
            writeCell(workbook, s, "spesa_fissa_media", formato(spesaFissa, "#,##0.00") + " " + EURO + "/mese");
            writeCell(workbook, s, "spesa_fissa_vendita", fs.getSpesaFissaVendita());
            writeCell(workbook, s, "spesa_fissa_vendita_media", formato(nvl(fs.getSpesaFissaVendita()), "#,##0.00") + " " + EURO + "/mese");
            writeCell(workbook, s, "spesa_fissa_rete", fs.getSpesaFissaRete());
            writeCell(workbook, s, "spesa_fissa_rete_media", formato(nvl(fs.getSpesaFissaRete()), "#,##0.00") + " " + EURO + "/mese");
            // quota potenza
            String potenzaMedia = fs.getSpesaPotenzaReteMedia() != null
                    ? formato(fs.getSpesaPotenzaReteMedia(), "#,##0.00") + " " + EURO + "/kW/mese" : "";
            writeCell(workbook, s, "picco_potenza", numero(fs.getPotenzaOneri()) + " kW per 1 mese");
            writeCell(workbook, s, "spesa_potenza", fs.getSpesaPotenzaRete());
            writeCell(workbook, s, "spesa_potenza_media", potenzaMedia);
            writeCell(workbook, s, "spesa_fissa_rete_2", fs.getSpesaPotenzaRete());
            writeCell(workbook, s, "spesa_fissa_rete_2_media", potenzaMedia);
            // accise, iva e totali
            writeCell(workbook, s, "iva", nvl(fs.getTotaleImposte()) + nvl(fs.getTotaleIva()));
            writeCell(workbook, s, "totale_bolletta", totaleBolletta);
            writeCell(workbook, s, "totale_da_pagare", totaleBolletta + nvl(fs.getTotalePartite()));
            if (!altrePartite(fs).isEmpty()) {
                writeCell(workbook, s, "altre_partite_01", altrePartite(fs));
                writeCell(workbook, s, "altre_partite_imp_01", fs.getTotalePartite());
            }
            // box dell'offerta
            writeCell(workbook, s, "nome_offerta", offerta.getNome());
            writeCell(workbook, s, "codice_offerta", offerta.getCodice());
            if (!offerta.isPun()) {
                writeCell(workbook, s, "box_tipologia_offerta", "Offerta a prezzo fisso");
                writeCell(workbook, s, "box_tipologia_prezzo", "Monoraria");
                writeCell(workbook, s, "box_formula", "Prezzo indicato");
            }
            if (offerta.getDataInizio() != null) writeCell(workbook, s, "data_inizio_offerta", offerta.getDataInizio());
            if (offerta.getDataFine() != null) writeCell(workbook, s, "data_fine_offerta", offerta.getDataFine());
            writeCell(workbook, s, "totale_offerta", formato(nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaFissaVendita()), "#,##0.0000") + " " + EURO);
            writeCell(workbook, s, "box_quota_consumi", "di cui spesa per la quota consumi " + formato(nvl(fs.getSpesaVendita()), "#,##0.00") + " " + EURO);
            writeCell(workbook, s, "box_quota_fissa", "di cui spesa per la quota fissa " + formato(nvl(fs.getSpesaFissaVendita()), "#,##0.00") + " " + EURO);
            writeCell(workbook, s, "box_valori_assoluti", String.format("%02d/%d: F1 %s %s/kWh -  F2 %s %s/kWh -  F3 %s %s/kWh",
                    fattura.getMese(), fattura.getAnno(), numero(fs.getPrezzoF1()), EURO, numero(fs.getPrezzoF2()), EURO,
                    numero(fs.getPrezzoF3()), EURO));
            // consumi fatturati
            LocalDate periodo = LocalDate.of(fattura.getAnno(), fattura.getMese(), 15);
            writeCell(workbook, s, "periodo_imposte", periodo);
            writeCell(workbook, s, "periodo_imposte_2", periodo);
            writeCell(workbook, s, "periodo_imposte_3", periodo);
            writeCell(workbook, s, "consumo_F1", fs.getConsumoF1());
            writeCell(workbook, s, "consumo_F2", fs.getConsumoF2());
            writeCell(workbook, s, "consumo_F3", fs.getConsumoF3());
            writeCell(workbook, s, "consumo_F1r", fs.getConsumoF1r());
            writeCell(workbook, s, "consumo_F2r", fs.getConsumoF2r());
            writeCell(workbook, s, "consumo_F3r", fs.getConsumoF3r());
            writeCell(workbook, s, "consumo_attiva_tot", fs.getConsumoTot());
            writeCell(workbook, s, "consumo_reattiva_tot", fs.getConsumoTotR());
            writeCell(workbook, s, "potenza_prelevata_F1", nvl(lettura.getPotF1()) * kp);
            writeCell(workbook, s, "potenza_prelevata_F2", nvl(lettura.getPotF2()) * kp);
            writeCell(workbook, s, "potenza_prelevata_F3", nvl(lettura.getPotF3()) * kp);
            // letture del contatore
            writeCell(workbook, s, "data_lettura_old", letturaOld.getDataLettura());
            writeCell(workbook, s, "tipo_lettura_old", tipoLettura(letturaOld.getTipoLettura()));
            writeCell(workbook, s, "lettura_F1_old", letturaOld.getEaF1());
            writeCell(workbook, s, "lettura_F2_old", letturaOld.getEaF2());
            writeCell(workbook, s, "lettura_F3_old", letturaOld.getEaF3());
            writeCell(workbook, s, "fattore_K_old", letturaOld.getKa());
            writeCell(workbook, s, "data_lettura", lettura.getDataLettura());
            writeCell(workbook, s, "tipo_lettura", tipoLettura(lettura.getTipoLettura()));
            writeCell(workbook, s, "lettura_F1", lettura.getEaF1());
            writeCell(workbook, s, "lettura_F2", lettura.getEaF2());
            writeCell(workbook, s, "lettura_F3", lettura.getEaF3());
            writeCell(workbook, s, "fattore_K", lettura.getKa());

            saveWorkbookToFile(workbook, fileName);
        }
    }

    // ------------------------------------------------------------------
    //  pagina 1: riepilogo
    // ------------------------------------------------------------------

    private void writePagina1(String fileName, Fattura fattura) throws IOException {
        String s = "riepilogo";
        Cliente cliente = fattura.getCliente();
        LocalDate dataFattura = fattura.getDataFattura();
        List<FatturaSingola> fattureSingole = fattura.getFattureSingole();
        double materia = nvl(fattura.getTotaleMateria());
        double trasportoOneri = nvl(fattura.getTotaleTrasporto()) + nvl(fattura.getTotaleOneri());
        double accise = nvl(fattura.getTotaleImposte());
        double partite = fattureSingole.stream().mapToDouble(fs -> nvl(fs.getTotalePartite())).sum();
        double iva = nvl(fattura.getTotaleIva());
        double totale = materia + trasportoOneri + accise + partite + iva;
        double consumo = nvl(fattura.getConsumoTot());
        String indirizzo2 = String.format("%05d", cliente.getCap()) + " " + cliente.getComune() + " (" + cliente.getProvincia() + ")";

        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(fileName))) {
            writeCell(workbook, s, "numero_fattura", fattura.getNumeroFattura());
            writeCell(workbook, s, "data_fattura", dataFattura);
            writeCell(workbook, s, "periodo_fatturazione", "Fattura di " + nomeMese(fattura.getMese()) + " " + fattura.getAnno());
            writeCell(workbook, s, "scadenza", "Scadenza " + YearMonth.from(dataFattura).atEndOfMonth().getDayOfMonth() + " "
                    + nomeMese(dataFattura.getMonthValue()) + " " + dataFattura.getYear());
            writeCell(workbook, s, "ragione_sociale", cliente.getRagioneSociale());
            writeCell(workbook, s, "indirizzo_1", cliente.getIndirizzo());
            writeCell(workbook, s, "indirizzo_2", indirizzo2);
            // sintesi degli importi
            writeCell(workbook, s, "totale_materia", materia);
            writeCell(workbook, s, "totale_oneri", trasportoOneri);
            writeCell(workbook, s, "totale_accise", accise);
            writeCell(workbook, s, "totale_partite", partite);
            writeCell(workbook, s, "totale_iva", iva);
            writeCell(workbook, s, "totale_fattura", totale);
            writeCell(workbook, s, "consumo_tot", consumo);
            writeCell(workbook, s, "totale_da_pagare", totale);
            writeCell(workbook, s, "importo_da_pagare", totale);
            // dati del grafico
            writeCell(workbook, s, "grafico_materia", materia);
            writeCell(workbook, s, "grafico_oneri", trasportoOneri);
            writeCell(workbook, s, "grafico_accise", accise);
            writeCell(workbook, s, "grafico_partite", partite);
            writeCell(workbook, s, "grafico_iva", iva);
            // dati cliente
            writeCell(workbook, s, "ragione_sociale_dati", cliente.getRagioneSociale());
            writeCell(workbook, s, "indirizzo_1_dati", cliente.getIndirizzo());
            writeCell(workbook, s, "indirizzo_2_dati", indirizzo2);
            writeCell(workbook, s, "partita_iva", cliente.getPIva());
            writeCell(workbook, s, "codice_cliente", codiceCliente(cliente));
            writeCell(workbook, s, "costo_medio_materia", consumo > 1 ? materia / consumo : null);
            writeCell(workbook, s, "costo_medio_bolletta", consumo > 1 ? totale / consumo : null);
            // riepilogo iva
            writeCell(workbook, s, "iva", fattureSingole.get(0).getFornitura().getIva() / 100);
            writeCell(workbook, s, "imponibile", materia + trasportoOneri + accise);
            writeCell(workbook, s, "importo_iva", iva);
            saveWorkbookToFile(workbook, fileName);
        }
    }

    public void createFattura(Fattura fattura) throws IOException {
        String numeroFattura = fattura.getNumeroFattura();
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        // nome file come nelle fatture di esempio: "<numero> - <ragione sociale>" senza caratteri non validi
        String nomeFile = (numeroFattura + " - " + fattura.getCliente().getRagioneSociale()).replaceAll("[<>|/*\\\\?\":.]", "").trim();
        String fatturaFileName = "fatture/" + anno + "/" + mese + "/" + nomeFile + ".xlsx";
        log.debug("Avviata creazione file excel: " + fatturaFileName);
        Workbook fatturaXls = createExcelFile();
        saveWorkbookToFile(fatturaXls, fatturaFileName);
        fatturaXls.close();
        for (FatturaSingola fatturaSingola : fattura.getFattureSingole()) {
            String pod = fatturaSingola.getFornitura().getId();
            addPagina("template/pagina_2.xlsx", fatturaFileName, pod + " letture");
            addPagina("template/pagina_3.xlsx", fatturaFileName, pod + " consumi");
            writeFornitura(fatturaFileName, fatturaSingola);
        }
        addPagina("template/pagina_1.xlsx", fatturaFileName, "riepilogo");
        writePagina1(fatturaFileName, fattura);

        save2Pdf(fatturaFileName);

        String pdf = fatturaFileName.substring(0, fatturaFileName.lastIndexOf(".")) + ".pdf";
        fattura.setPdf(pdf);
        fattura.setXlsx(fatturaFileName);
        fatturaRepository.save(fattura);
    }

    // ------------------------------------------------------------------
    //  utilità
    // ------------------------------------------------------------------

    /**
     * Descrizione delle altre partite della fornitura (nel template c'è una sola riga).
     */
    private static String altrePartite(FatturaSingola fs) {
        return fs.getVoci().stream()
                .filter(v -> v.getSezione() == SezioneFattura.ALTRE_PARTITE)
                .map(VoceFatturaSingola::getDescrizione)
                .collect(java.util.stream.Collectors.joining(" - "));
    }

    private static String codiceCliente(Cliente cliente) {
        return "2050" + cliente.getId();
    }

    private static String tipoLettura(TipoLettura tipo) {
        if (tipo == null) return "";
        return switch (tipo) {
            case REALE -> "Effettiva";
            case STIMA -> "Stimata";
            case RETTIFICA -> "Rettifica";
            case AUTOLETTURA -> "Autolettura";
            case CAMBIO -> "Cambio contatore";
        };
    }

    private static String prezzoMedio(Double valore, String unita) {
        return valore != null ? formato(valore, "0.000000") + " " + EURO + unita : "";
    }

    private static String nomeMese(int mese) {
        return capitalize(Month.of(mese).getDisplayName(TextStyle.FULL, IT));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(IT) + s.substring(1).toLowerCase(IT);
    }

    /**
     * Numero in formato italiano con il pattern indicato (es. "#,##0.000" -> 1.234,567).
     */
    private static String formato(double valore, String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(IT)).format(valore);
    }

    /**
     * Numero in formato italiano senza zeri finali (es. 28.0 -> "28", 0.238375 -> "0,238375").
     */
    private static String numero(Double valore) {
        if (valore == null) return "";
        return BigDecimal.valueOf(valore).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static double nvl(Double valore) {
        return valore != null ? valore : 0;
    }
}
