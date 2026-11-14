package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.SezioneFattura;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    // righe del dettaglio nascoste quando l'importo è 0 (es. la rettifica, che hanno solo alcune forniture)
    private static final java.util.Set<String> RIGHE_NASCOSTE_SE_ZERO = java.util.Set.of("rettifica");

    private static final String TEMPLATE_RIEPILOGO = "template/pagina_1.xlsx";
    private static final String TEMPLATE_SCONTRINO = "template/pagina_2.xlsx";
    private static final String TEMPLATE_DETTAGLIO = "template/pagina_3.xlsx";

    @Autowired
    FatturaRepository fatturaRepository;


    /**
     * Esegue uno script python che pilota Excel. Se fallisce interrompe la generazione della fattura,
     * invece di produrre un file incompleto.
     */
    private void eseguiScript(String script, String... args) {
        eseguiScript(script, null, args);
    }

    /**
     * Come sopra, passando ogni riga dell'output dello script a perRiga (se non null) appena viene stampata.
     */
    private void eseguiScript(String script, java.util.function.Consumer<String> perRiga, String... args) {
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
                    if (perRiga != null) perRiga.accept(line);
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
     * Compila il dettaglio (pagina 3) e lo scontrino (pagina 2) di una fornitura, ciascuno nel proprio workbook
     * creato dal template con il foglio già rinominato.
     */
    private void writeFornitura(Workbook dettaglio, Workbook scontrino, FatturaSingola fs) {
        Fornitura fornitura = fs.getFornitura();
        Fattura fattura = fs.getFattura();
        Offerta offerta = fornitura.getOfferta();
        Lettura lettura = fs.getLetturaFinale();
        Lettura letturaOld = fs.getLetturaIniziale();
        double kp = lettura.getKp() != null ? lettura.getKp() : 1;

        // ---------------- pagina 3: dettaglio ----------------
        String s = fornitura.getId() + " consumi";
        writeCell(dettaglio, s, "codice_cliente", codiceCliente(fornitura.getCliente()));
        writeCell(dettaglio, s, "pod", fornitura.getId());
        writeCell(dettaglio, s, "potenza_disponibile", fornitura.getPotenzaDisponibile());
        writeCell(dettaglio, s, "tipo_misuratore", capitalize(fornitura.getTipoContatore().name()));
        writeCell(dettaglio, s, "potenza_impegnata", fs.getPotenzaOneri());
        for (VoceFatturaSingola voce : fs.getVoci()) {
            if (voce.getSezione() == SezioneFattura.ALTRE_PARTITE) continue; // riga unica, scritta sotto
            String nome = "voce_" + voce.getCodice();
            Cell cellaVoce = findCell(dettaglio, s, nome);
            if (cellaVoce == null) continue;
            if (RIGHE_NASCOSTE_SE_ZERO.contains(voce.getCodice()) && nvl(voce.getImporto()) == 0) {
                // riga presente solo per alcune forniture: nascosta, non compare nel pdf
                cellaVoce.getRow().setZeroHeight(true);
                continue;
            }
            setValue(findCell(dettaglio, s, nome, COL_DESCRIZIONE), voce.getDescrizione());
            setValue(findCell(dettaglio, s, nome, COL_DAL), voce.getDal());
            setValue(findCell(dettaglio, s, nome, COL_AL), voce.getAl());
            setValue(findCell(dettaglio, s, nome, COL_UNITA), voce.getUnita());
            setValue(findCell(dettaglio, s, nome, COL_UNITARIO), voce.getUnitario());
            setValue(findCell(dettaglio, s, nome, COL_QUANTITA), voce.getQuantita());
            setValue(findCell(dettaglio, s, nome, COL_IMPORTO), voce.getImporto());
            setValue(findCell(dettaglio, s, nome, COL_IVA), voce.getIva());
        }
        writeCell(dettaglio, s, "totale_materia", fs.getTotaleMateria());
        writeCell(dettaglio, s, "totale_trasporto", fs.getTotaleTrasporto());
        writeCell(dettaglio, s, "totale_oneri", fs.getTotaleOneri());
        writeCell(dettaglio, s, "totale_accise", fs.getTotaleImposte());
        writeCell(dettaglio, s, "altre_partite_1", altrePartite(fs));
        writeCell(dettaglio, s, "totale_partite", fs.getTotalePartite());
        // aliquota della riga altre partite: 0% se sono tutte esenti iva
        boolean partiteSoggette = fs.getVoci().stream()
                .anyMatch(v -> v.getSezione() == SezioneFattura.ALTRE_PARTITE && nvl(v.getIva()) > 0);
        Cell ivaPartite = findCell(dettaglio, s, "altre_partite_1", COL_IVA);
        if (ivaPartite != null) setValue(ivaPartite, partiteSoggette ? fornitura.getIva() / 100 : 0.0);
        writeCell(dettaglio, s, "aliquota_iva", fornitura.getIva() / 100);
        writeCell(dettaglio, s, "imponibile", fs.getTotaleImponibile());
        writeCell(dettaglio, s, "totale_iva", fs.getTotaleIva());

        // ---------------- pagina 2: scontrino ----------------
        s = fornitura.getId() + " letture";
        double consumo = nvl(fs.getConsumoTot());
        double spesaFissa = nvl(fs.getSpesaFissaVendita()) + nvl(fs.getSpesaFissaRete());
        // imponibile + iva: comprende anche le voci che non confluiscono nelle righe dello scontrino
        double totaleBolletta = nvl(fs.getTotaleDaPagare()) - nvl(fs.getTotalePartite());
        writeCell(scontrino, s, "pod", fornitura.getId());
        writeCell(scontrino, s, "indirizzo_fornitura", fornitura.getIndirizzo() + ", " + fornitura.getCap() + " "
                + fornitura.getComune() + " (" + fornitura.getProvincia() + ")");
        writeCell(scontrino, s, "potenza_impegnata", numero(fornitura.getPotenzaImpegnata()) + " kW");
        // quota per consumi
        writeCell(scontrino, s, "consumo_netto", formato(consumo, "#,##0.000") + " kWh");
        writeCell(scontrino, s, "spesa_consumi", nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaRete()));
        writeCell(scontrino, s, "spesa_media", prezzoMedio(fs.getSpesaMedia(), "/kWh"));
        writeCell(scontrino, s, "spesa_vendita", fs.getSpesaVendita());
        writeCell(scontrino, s, "spesa_vendita_media", prezzoMedio(fs.getSpesaVenditaMedia(), "/kWh"));
        writeCell(scontrino, s, "spesa_rete", fs.getSpesaRete());
        writeCell(scontrino, s, "spesa_rete_media", prezzoMedio(fs.getSpesaReteMedia(), "/kWh"));
        // quota fissa (1 mese)
        writeCell(scontrino, s, "spesa_fissa", spesaFissa);
        writeCell(scontrino, s, "spesa_fissa_media", formato(spesaFissa, "#,##0.00") + " " + EURO + "/mese");
        writeCell(scontrino, s, "spesa_fissa_vendita", fs.getSpesaFissaVendita());
        writeCell(scontrino, s, "spesa_fissa_vendita_media", formato(nvl(fs.getSpesaFissaVendita()), "#,##0.00") + " " + EURO + "/mese");
        writeCell(scontrino, s, "spesa_fissa_rete", fs.getSpesaFissaRete());
        writeCell(scontrino, s, "spesa_fissa_rete_media", formato(nvl(fs.getSpesaFissaRete()), "#,##0.00") + " " + EURO + "/mese");
        // quota potenza
        String potenzaMedia = fs.getSpesaPotenzaReteMedia() != null
                ? formato(fs.getSpesaPotenzaReteMedia(), "#,##0.00") + " " + EURO + "/kW/mese" : "";
        writeCell(scontrino, s, "picco_potenza", numero(fs.getPotenzaOneri()) + " kW per 1 mese");
        writeCell(scontrino, s, "spesa_potenza", fs.getSpesaPotenzaRete());
        writeCell(scontrino, s, "spesa_potenza_media", potenzaMedia);
        writeCell(scontrino, s, "spesa_fissa_rete_2", fs.getSpesaPotenzaRete());
        writeCell(scontrino, s, "spesa_fissa_rete_2_media", potenzaMedia);
        // accise, iva e totali
        writeCell(scontrino, s, "iva", nvl(fs.getTotaleImposte()) + nvl(fs.getTotaleIva()));
        writeCell(scontrino, s, "totale_bolletta", totaleBolletta);
        writeCell(scontrino, s, "totale_da_pagare", fs.getTotaleDaPagare());
        if (!altrePartite(fs).isEmpty()) {
            writeCell(scontrino, s, "altre_partite_01", altrePartite(fs));
            writeCell(scontrino, s, "altre_partite_imp_01", fs.getTotalePartite());
        }
        // box dell'offerta
        writeCell(scontrino, s, "nome_offerta", offerta.getNome());
        writeCell(scontrino, s, "codice_offerta", offerta.getCodice());
        if (!offerta.isPun()) {
            writeCell(scontrino, s, "box_tipologia_offerta", "Offerta a prezzo fisso");
            writeCell(scontrino, s, "box_tipologia_prezzo", "Monoraria");
            writeCell(scontrino, s, "box_formula", "Prezzo indicato");
        }
        if (offerta.getDataInizio() != null) writeCell(scontrino, s, "data_inizio_offerta", offerta.getDataInizio());
        if (offerta.getDataFine() != null) writeCell(scontrino, s, "data_fine_offerta", offerta.getDataFine());
        writeCell(scontrino, s, "totale_offerta", formato(nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaFissaVendita()), "#,##0.0000") + " " + EURO);
        writeCell(scontrino, s, "box_quota_consumi", "di cui spesa per la quota consumi " + formato(nvl(fs.getSpesaVendita()), "#,##0.00") + " " + EURO);
        writeCell(scontrino, s, "box_quota_fissa", "di cui spesa per la quota fissa " + formato(nvl(fs.getSpesaFissaVendita()), "#,##0.00") + " " + EURO);
        writeCell(scontrino, s, "box_valori_assoluti", String.format("%02d/%d: F1 %s %s/kWh -  F2 %s %s/kWh -  F3 %s %s/kWh",
                fattura.getMese(), fattura.getAnno(), numero(fs.getPrezzoF1()), EURO, numero(fs.getPrezzoF2()), EURO,
                numero(fs.getPrezzoF3()), EURO));
        // consumi fatturati
        LocalDate periodo = LocalDate.of(fattura.getAnno(), fattura.getMese(), 15);
        writeCell(scontrino, s, "periodo_imposte", periodo);
        writeCell(scontrino, s, "periodo_imposte_2", periodo);
        writeCell(scontrino, s, "periodo_imposte_3", periodo);
        writeCell(scontrino, s, "consumo_F1", fs.getConsumoF1());
        writeCell(scontrino, s, "consumo_F2", fs.getConsumoF2());
        writeCell(scontrino, s, "consumo_F3", fs.getConsumoF3());
        writeCell(scontrino, s, "consumo_F1r", fs.getConsumoF1r());
        writeCell(scontrino, s, "consumo_F2r", fs.getConsumoF2r());
        writeCell(scontrino, s, "consumo_F3r", fs.getConsumoF3r());
        writeCell(scontrino, s, "consumo_attiva_tot", fs.getConsumoTot());
        writeCell(scontrino, s, "consumo_reattiva_tot", fs.getConsumoTotR());
        writeCell(scontrino, s, "potenza_prelevata_F1", nvl(lettura.getPotF1()) * kp);
        writeCell(scontrino, s, "potenza_prelevata_F2", nvl(lettura.getPotF2()) * kp);
        writeCell(scontrino, s, "potenza_prelevata_F3", nvl(lettura.getPotF3()) * kp);
        // letture del contatore
        writeCell(scontrino, s, "data_lettura_old", letturaOld.getDataLettura());
        writeCell(scontrino, s, "tipo_lettura_old", tipoLettura(letturaOld.getTipoLettura()));
        writeCell(scontrino, s, "lettura_F1_old", letturaOld.getEaF1());
        writeCell(scontrino, s, "lettura_F2_old", letturaOld.getEaF2());
        writeCell(scontrino, s, "lettura_F3_old", letturaOld.getEaF3());
        writeCell(scontrino, s, "fattore_K_old", letturaOld.getKa());
        writeCell(scontrino, s, "data_lettura", lettura.getDataLettura());
        writeCell(scontrino, s, "tipo_lettura", tipoLettura(lettura.getTipoLettura()));
        writeCell(scontrino, s, "lettura_F1", lettura.getEaF1());
        writeCell(scontrino, s, "lettura_F2", lettura.getEaF2());
        writeCell(scontrino, s, "lettura_F3", lettura.getEaF3());
        writeCell(scontrino, s, "fattore_K", lettura.getKa());
    }

    // ------------------------------------------------------------------
    //  pagina 1: riepilogo
    // ------------------------------------------------------------------

    private void writePagina1(Workbook workbook, Fattura fattura) {
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
        // comprende le altre partite soggette a iva
        writeCell(workbook, s, "imponibile", nvl(fattura.getTotaleImponibile()));
        writeCell(workbook, s, "importo_iva", iva);
    }

    /**
     * Fattura pronta da comporre: le pagine compilate (file temporanei) e i file da produrre.
     */
    public record LavoroFattura(Long fatturaId, String xlsx, String pdf, List<String> pagine, Path cartella) {
    }

    /**
     * Genera xlsx e pdf di una fattura e li registra sulla fattura.
     */
    public void createFattura(Fattura fattura) throws IOException {
        LavoroFattura lavoro = preparaFattura(fattura);
        String errore = componiFatture(List.of(lavoro)).get(0);
        if (errore != null) {
            throw new RuntimeException("Composizione della fattura " + fattura.getNumeroFattura() + " non riuscita:\n" + errore);
        }
        fattura.setXlsx(lavoro.xlsx());
        fattura.setPdf(lavoro.pdf());
        fatturaRepository.save(fattura);
    }

    /**
     * Compila con POI ogni pagina della fattura in un file temporaneo creato dal template (solo Java, niente Excel):
     * può essere eseguito in parallelo per più fatture.
     */
    public LavoroFattura preparaFattura(Fattura fattura) throws IOException {
        int mese = fattura.getMese();
        int anno = fattura.getAnno();
        // nome file come nelle fatture di esempio: "<numero> - <ragione sociale>" senza caratteri non validi
        String nomeFile = (fattura.getNumeroFattura() + " - " + fattura.getCliente().getRagioneSociale()).replaceAll("[<>|/*\\\\?\":.]", "").trim();
        String xlsx = "fatture/" + anno + "/" + mese + "/" + nomeFile + ".xlsx";
        String pdf = "fatture/" + anno + "/" + mese + "/" + nomeFile + ".pdf";
        Files.createDirectories(Path.of(xlsx).getParent());

        Path cartella = Files.createTempDirectory("fattura_");
        try {
            List<String> pagine = new ArrayList<>();
            Workbook riepilogo = apriTemplate(TEMPLATE_RIEPILOGO, "riepilogo");
            writePagina1(riepilogo, fattura);
            pagine.add(salva(riepilogo, cartella, pagine.size()));
            for (FatturaSingola fatturaSingola : fattura.getFattureSingole()) {
                String pod = fatturaSingola.getFornitura().getId();
                Workbook scontrino = apriTemplate(TEMPLATE_SCONTRINO, pod + " letture");
                Workbook dettaglio = apriTemplate(TEMPLATE_DETTAGLIO, pod + " consumi");
                writeFornitura(dettaglio, scontrino, fatturaSingola);
                pagine.add(salva(scontrino, cartella, pagine.size()));
                pagine.add(salva(dettaglio, cartella, pagine.size()));
            }
            return new LavoroFattura(fattura.getId(), xlsx, pdf, pagine, cartella);
        } catch (IOException | RuntimeException e) {
            eliminaCartella(cartella);
            throw e;
        }
    }

    /**
     * Compone xlsx e pdf delle fatture con un solo processo python e una sola istanza di Excel,
     * una fattura dopo l'altra. Elimina i file temporanei delle pagine.
     *
     * @return per ogni lavoro, nello stesso ordine, il messaggio d'errore o null se la fattura è stata composta
     */
    public List<String> componiFatture(List<LavoroFattura> lavori) throws IOException {
        return componiFatture(lavori, null);
    }

    /**
     * Come sopra, chiamando composta ogni volta che lo script termina una fattura (riuscita o no).
     */
    public List<String> componiFatture(List<LavoroFattura> lavori, Runnable composta) throws IOException {
        Path cartella = Files.createTempDirectory("componi_");
        try {
            ObjectMapper json = new ObjectMapper();
            Path fileLavori = cartella.resolve("lavori.json");
            Path fileEsiti = cartella.resolve("esiti.json");
            json.writeValue(fileLavori.toFile(), lavori.stream()
                    .map(l -> Map.of("xlsx", l.xlsx(), "pdf", l.pdf(), "pagine", l.pagine()))
                    .toList());
            log.debug("Composizione di {} fatture", lavori.size());
            // lo script stampa "ok ..." o "ERRORE ..." alla fine di ogni fattura
            eseguiScript("script/componiFattura.py",
                    riga -> {
                        if (composta != null && (riga.startsWith("ok ") || riga.startsWith("ERRORE "))) composta.run();
                    },
                    fileLavori.toString(), fileEsiti.toString());
            List<Map<String, Object>> esiti = json.readValue(fileEsiti.toFile(), new TypeReference<>() {
            });
            List<String> errori = new ArrayList<>();
            for (int i = 0; i < lavori.size(); i++) {
                Map<String, Object> esito = i < esiti.size() ? esiti.get(i) : null;
                if (esito == null) errori.add("Fattura non composta");
                else errori.add(Boolean.TRUE.equals(esito.get("ok")) ? null : String.valueOf(esito.get("errore")));
            }
            return errori;
        } finally {
            eliminaCartella(cartella);
            lavori.forEach(l -> eliminaCartella(l.cartella()));
        }
    }

    private static void eliminaCartella(Path cartella) {
        try (var files = Files.walk(cartella)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        } catch (IOException e) {
            log.warn("Impossibile eliminare la cartella temporanea {}", cartella);
        }
    }

    /**
     * Carica il template in memoria e rinomina il suo unico foglio (POI aggiorna anche le celle con nome).
     */
    private Workbook apriTemplate(String template, String nomeFoglio) throws IOException {
        try (InputStream in = new FileInputStream(template)) {
            Workbook workbook = WorkbookFactory.create(in);
            workbook.setSheetName(0, nomeFoglio);
            return workbook;
        }
    }

    private String salva(Workbook workbook, Path cartella, int indice) throws IOException {
        Path file = cartella.resolve(String.format("pagina_%03d.xlsx", indice));
        try (workbook; OutputStream out = Files.newOutputStream(file)) {
            workbook.write(out);
        }
        return file.toString();
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
