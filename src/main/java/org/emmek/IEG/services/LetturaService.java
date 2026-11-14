package org.emmek.IEG.services;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.emmek.IEG.configs.JaxbParser;
import org.emmek.IEG.entities.Fornitura;
import org.emmek.IEG.entities.Lettura;
import org.emmek.IEG.enums.TipoContatore;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.exceptions.NotFoundException;
import org.emmek.IEG.model.xml.DatiPod;
import org.emmek.IEG.model.xml.FlussoMisure;
import org.emmek.IEG.model.Avanzamento;
import org.emmek.IEG.payloads.ConsumoMensileDTO;
import org.emmek.IEG.payloads.EsitoImportLettureDTO;
import org.emmek.IEG.payloads.LetturaDTO;
import org.emmek.IEG.repositories.FatturaSingolaRepository;
import org.emmek.IEG.repositories.LetturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class LetturaService {

    // perdite di rete in percentuale sul consumo
    private static final double PERCENTUALE_PERDITE = 10;

    @Autowired
    private LetturaRepository letturaRepository;

    @Autowired
    private FatturaSingolaRepository fatturaSingolaRepository;

    @Autowired
    private FornituraService fornituraService;

    @Autowired
//    private Function<String, FlussoMisure> parseXmlFunction;
    private JaxbParser jaxbParser;

    public Page<Lettura> findAll(int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        return letturaRepository.findAll(pageable);
    }

    /**
     * Esito di una misura del flusso (vedi parseDatiPod).
     */
    public enum EsitoMisura {INSERITA, AGGIORNATA, SWITCH_MANTENUTA, POD_SCONOSCIUTO, ERRORE}

    /**
     * Copia lo zip caricato in un file temporaneo: il file della richiesta sparisce a fine richiesta, mentre
     * l'import prosegue in background (importaFlussi).
     */
    public File salvaZip(MultipartFile body) throws IOException {
        File zip = File.createTempFile(UUID.randomUUID().toString(), "_tmp.zip");
        log.debug("zip: {}", zip.getAbsolutePath());
        try (FileOutputStream o = new FileOutputStream(zip)) {
            IOUtils.copy(body.getInputStream(), o);
        }
        return zip;
    }

    /**
     * Estrae lo zip (anche annidato) in data/uploads e importa le letture dei flussi PDO, PNO e SNM; gli errori sui
     * singoli file e misure sono contati e non interrompono l'import. Lo zip viene eliminato alla fine.
     */
    public EsitoImportLettureDTO importaFlussi(File zip, Avanzamento avanzamento) throws IOException {
        String destination = "data/uploads";
        try {
            avanzamento.fase("Estrazione archivi", 0);
            deleteAllFilesInFolder(destination);
            try (ZipFile zipFile = new ZipFile(zip)) {
                zipFile.extractAll(destination);
            } catch (Exception e) {
                throw new IOException("Archivio zip non leggibile: " + e.getMessage(), e);
            }
            processZipFilesRecursively(new File(destination), destination);
            log.debug("estrazione completata");

            List<File> flussi = new ArrayList<>();
            raccogliFlussi(new File(destination), flussi);
            avanzamento.fase("Importazione letture", flussi.size());
            Map<EsitoMisura, Integer> conteggi = new EnumMap<>(EsitoMisura.class);
            Set<String> podSconosciuti = new TreeSet<>();
            int fileScartati = 0;
            for (File file : flussi) {
                log.debug("Processing XML file: {}", file.getAbsolutePath());
                try {
                    FlussoMisure flusso = jaxbParser.parseXml(file.getAbsolutePath());
                    for (DatiPod datiPod : flusso.datiPod) {
                        EsitoMisura esito = parseDatiPod(datiPod, isFlussoSwitch(flusso.codFlusso));
                        conteggi.merge(esito, 1, Integer::sum);
                        if (esito == EsitoMisura.POD_SCONOSCIUTO) podSconosciuti.add(datiPod.pod);
                    }
                } catch (Exception e) {
                    log.error("Errore durante il parsing del file XML: {}", file.getName());
                    fileScartati++;
                    avanzamento.errore(file.getName() + ": " + e.getMessage());
                }
                avanzamento.avanza();
            }
            EsitoImportLettureDTO esito = new EsitoImportLettureDTO(flussi.size(), fileScartati,
                    conteggi.getOrDefault(EsitoMisura.INSERITA, 0),
                    conteggi.getOrDefault(EsitoMisura.AGGIORNATA, 0),
                    conteggi.getOrDefault(EsitoMisura.SWITCH_MANTENUTA, 0),
                    conteggi.getOrDefault(EsitoMisura.ERRORE, 0),
                    List.copyOf(podSconosciuti));
            log.info("Import flussi terminato: {}", esito);
            return esito;
        } finally {
            if (!zip.delete()) log.warn("Impossibile eliminare lo zip temporaneo {}", zip);
        }
    }

    /**
     * File xml dei flussi di misura importati (PDO, PNO, SNM) nella cartella e nelle sottocartelle.
     */
    private void raccogliFlussi(File folder, List<File> flussi) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                raccogliFlussi(file, flussi);
            } else if (file.getName().toLowerCase().endsWith(".xml") &&
                    (file.getName().contains("_PDO") || file.getName().contains("_PNO") || file.getName().contains("_SNM"))) {
                flussi.add(file);
            }
        }
    }

    private void processZipFilesRecursively(File folder, String destination) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processZipFilesRecursively(file, destination);
                } else if (file.getName().toLowerCase().endsWith(".zip")) {
                    try (ZipFile innerZipFile = new ZipFile(file)) {
                        innerZipFile.extractAll(destination);
                    }
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.error("Impossibile eliminare il file ZIP: {}", file.getName());
                    }
                }
            }
        }
    }

    /**
     * Flussi di switching (SNM, SNM2G) e relative rettifiche (RSN, RSN2G).
     */
    private static boolean isFlussoSwitch(String codFlusso) {
        return codFlusso != null && (codFlusso.startsWith("SNM") || codFlusso.startsWith("RSN"));
    }

    public EsitoMisura parseDatiPod(DatiPod datiPod, boolean switching) {
        // le misure di un flusso di switching sono letture di switch anche se Raccolta non vale "S"
        if (switching && datiPod.misura != null) datiPod.misura.raccolta = Lettura.RACCOLTA_SWITCH;
        Lettura lettura;
        Fornitura fornitura = null;
        try {
            fornitura = fornituraService.finById(datiPod.pod);
        } catch (Exception ignore) {
        }
        if (fornitura != null) {
            int giorno;
            int mese;
            int anno;

            if (datiPod.dataMisura != null) {
                // flussi di switch, voltura e prestazioni tecniche: la data della misura è DataMisura
                // (per lo switch il giorno prima della data di switch), anche se è presente la curva Ea
                giorno = Integer.parseInt(datiPod.dataMisura.substring(0, 2));
                mese = Integer.parseInt(datiPod.dataMisura.substring(3, 5));
                anno = Integer.parseInt(datiPod.dataMisura.substring(6, 10));
            } else {
                // flussi periodici: giorno dalla curva Ea, mese e anno da MeseAnno
                if (datiPod.misura.ea != null) {
                    giorno = Integer.parseInt(datiPod.misura.ea.get(0).valore);
                } else {
                    giorno = Integer.parseInt(datiPod.dataPrest.substring(0, 2));
                }
                if (datiPod.meseAnno != null) {
                    mese = Integer.parseInt(datiPod.meseAnno.substring(0, 2));
                    anno = Integer.parseInt(datiPod.meseAnno.substring(3, 7));
                } else {
                    mese = Integer.parseInt(datiPod.dataPrest.substring(3, 5));
                    anno = Integer.parseInt(datiPod.dataPrest.substring(6, 10));
                }
            }

            //  && datiPod.misura.validato.equals("S")
//            if (giorno == 30 && mese == 9 && anno == 2025) {
//            if (true) {
            try {
//                    log.info("parsing... {} {}", datiPod.pod, fornitura.getCliente().getRagioneSociale());
                LocalDate dataLettura = LocalDate.of(anno, mese, giorno);
                List<Lettura> lettureEsistenti = letturaRepository.findByFornituraAndDataLettura(fornitura, dataLettura);
                lettura = lettureEsistenti.isEmpty() ? null : lettureEsistenti.get(0);
                boolean nuova = lettura == null;
                if (nuova) {
                    lettura = new Lettura();
                    lettura.setId(getNextId());
                    lettura.setFornitura(fornitura);
                    lettura.setDataLettura(LocalDate.of(anno, mese, giorno));
                    log.info("inserisco -------> lettura pod {} del {}/{}/{} - {}", datiPod.pod, giorno, mese, anno, fornitura.getCliente().getRagioneSociale());
                } else if (lettura.isSwitch() && !Lettura.RACCOLTA_SWITCH.equals(datiPod.misura.raccolta)) {
                    // la lettura di switch non viene sovrascritta da una lettura periodica dello stesso giorno
                    log.debug("lettura di switch del pod {} del {}/{}/{} mantenuta", datiPod.pod, giorno, mese, anno);
                    return EsitoMisura.SWITCH_MANTENUTA;
                }

                switch (datiPod.datiPdp.trattamento) {
                    case "O" -> lettura.setTipoContatore(TipoContatore.ORARIO);
                    case "F" -> lettura.setTipoContatore(TipoContatore.FASCIA);
                    case "M" -> lettura.setTipoContatore(TipoContatore.MONORARIO);
                }
                lettura.setUtile(true);
                switch (datiPod.misura.tipoDato) {
                    case "E" -> lettura.setTipoLettura(TipoLettura.REALE);
                    case "S" -> lettura.setTipoLettura(TipoLettura.STIMA);
                    case "A" -> lettura.setTipoLettura(TipoLettura.AUTOLETTURA);
                }
                lettura.setRaccolta(datiPod.misura.raccolta);
                lettura.setTipoDato(datiPod.misura.tipoDato);
                lettura.setCausaOstativa(datiPod.misura.causaOstativa);
                lettura.setValidato(datiPod.misura.validato);
                if (datiPod.misura.potMax != null) {
                    lettura.setPotMax(datiPod.misura.potMax.replaceAll(",", "."));
                }
                lettura.setEaF1(Double.parseDouble(datiPod.misura.eaF1.replaceAll(",", ".")));
                lettura.setEaF2(Double.parseDouble(datiPod.misura.eaF2.replaceAll(",", ".")));
                lettura.setEaF3(Double.parseDouble(datiPod.misura.eaF3.replaceAll(",", ".")));
                lettura.setErF1(Double.parseDouble(datiPod.misura.erF1.replaceAll(",", ".")));
                lettura.setErF2(Double.parseDouble(datiPod.misura.erF2.replaceAll(",", ".")));
                lettura.setErF3(Double.parseDouble(datiPod.misura.erF3.replaceAll(",", ".")));
                lettura.setErcF1(parseDecimale(datiPod.misura.ercF1));
                lettura.setErcF2(parseDecimale(datiPod.misura.ercF2));
                lettura.setErcF3(parseDecimale(datiPod.misura.ercF3));
                lettura.setPotF1(Double.parseDouble(datiPod.misura.potF1.replaceAll(",", ".")));
                lettura.setPotF2(Double.parseDouble(datiPod.misura.potF2.replaceAll(",", ".")));
                lettura.setPotF3(Double.parseDouble(datiPod.misura.potF3.replaceAll(",", ".")));
                lettura.setKa(Double.parseDouble(datiPod.datiPdp.ka.replaceAll(",", ".")));
                lettura.setKr(Double.parseDouble(datiPod.datiPdp.kr.replaceAll(",", ".")));
                lettura.setKp(Double.parseDouble(datiPod.datiPdp.kp.replaceAll(",", ".")));

//                    log.debug(datiPod.pod + " " + fornitura.getCliente().getRagioneSociale());
//                    log.info("lettura del {}/{}/{}", giorno, mese, anno);

                letturaRepository.save(lettura);
                return nuova ? EsitoMisura.INSERITA : EsitoMisura.AGGIORNATA;
            } catch (Exception e) {
                log.error("lettura non importata da file xml - {}", e.getMessage());
                return EsitoMisura.ERRORE;
            }
//            }
        }
        return EsitoMisura.POD_SCONOSCIUTO;
    }

    private void deleteAllFilesInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteAllFilesInFolder(f.getAbsolutePath()); // Elimina i contenuti della directory
                }
                boolean isDeleted = f.delete(); // Elimina il file o la directory vuota
                if (!isDeleted) {
                    log.error("Failed to delete: {}", f.getName());
                }
            }
        }
    }

    public Long getNextId() {
        Long maxId = letturaRepository.findMaxId();
        return (maxId == null ? 1 : maxId + 1);
    }

    public void save(Lettura lettura) {
        letturaRepository.save(lettura);
    }

    public Lettura save(LetturaDTO body) {
        Lettura lettura = new Lettura();
        Fornitura fornitura = null;
        try {
            fornitura = fornituraService.finById(body.pod());
        } catch (Exception ignore) {
        }
        lettura.setId(getNextId());
        lettura.setFornitura(fornitura);
        lettura.setDataLettura(LocalDate.parse(body.dataLettura()));
        lettura.setTipoContatore(TipoContatore.valueOf(body.tipoContatore()));
        lettura.setUtile(true);
        lettura.setTipoLettura(TipoLettura.valueOf(body.tipoLettura()));
        lettura.setRaccolta(body.raccolta());
        lettura.setTipoDato(body.tipoDato());
        lettura.setValidato(body.validato());
        Double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        Double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        Double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        Double potMax = Math.max(potF1, Math.max(potF2, potF3));
        lettura.setPotMax(String.valueOf(potMax));
        lettura.setEaF1(Double.parseDouble(body.eaF1().replaceAll(",", ".")));
        lettura.setEaF2(Double.parseDouble(body.eaF2().replaceAll(",", ".")));
        lettura.setEaF3(Double.parseDouble(body.eaF3().replaceAll(",", ".")));
        lettura.setErF1(Double.parseDouble(body.erF1().replaceAll(",", ".")));
        lettura.setErF2(Double.parseDouble(body.erF2().replaceAll(",", ".")));
        lettura.setErF3(Double.parseDouble(body.erF3().replaceAll(",", ".")));
        lettura.setPotF1(potF1);
        lettura.setPotF2(potF2);
        lettura.setPotF3(potF3);
        lettura.setKa(Double.parseDouble(body.ka().replaceAll(",", ".")));
        lettura.setKr(Double.parseDouble(body.kr().replaceAll(",", ".")));
        lettura.setKp(Double.parseDouble(body.kp().replaceAll(",", ".")));
        lettura.setNote(body.note());

        return letturaRepository.save(lettura);
    }

    public void delete(Long id) {
        Lettura lettura = letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));
        letturaRepository.delete(lettura);
    }

    public Lettura get(Long id) {
        return letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));

    }

    public Lettura update(Long id, LetturaDTO body) {
        Lettura lettura = letturaRepository.findById(id).orElseThrow(() -> new NotFoundException("Lettura non trovata"));
        lettura.setDataLettura(LocalDate.parse(body.dataLettura()));
        lettura.setTipoContatore(TipoContatore.valueOf(body.tipoContatore()));
        lettura.setUtile(true);
        lettura.setTipoLettura(TipoLettura.valueOf(body.tipoLettura()));
        lettura.setRaccolta(body.raccolta());
        lettura.setTipoDato(body.tipoDato());
        lettura.setValidato(body.validato());
        Double potF1 = Double.parseDouble(body.potF1().replaceAll(",", "."));
        Double potF2 = Double.parseDouble(body.potF2().replaceAll(",", "."));
        Double potF3 = Double.parseDouble(body.potF3().replaceAll(",", "."));
        Double potMax = Math.max(potF1, Math.max(potF2, potF3));
        lettura.setPotMax(String.valueOf(potMax));
        lettura.setEaF1(Double.parseDouble(body.eaF1().replaceAll(",", ".")));
        lettura.setEaF2(Double.parseDouble(body.eaF2().replaceAll(",", ".")));
        lettura.setEaF3(Double.parseDouble(body.eaF3().replaceAll(",", ".")));
        lettura.setErF1(Double.parseDouble(body.erF1().replaceAll(",", ".")));
        lettura.setErF2(Double.parseDouble(body.erF2().replaceAll(",", ".")));
        lettura.setErF3(Double.parseDouble(body.erF3().replaceAll(",", ".")));
        lettura.setPotF1(potF1);
        lettura.setPotF2(potF2);
        lettura.setPotF3(potF3);
        lettura.setKa(Double.parseDouble(body.ka().replaceAll(",", ".")));
        lettura.setKr(Double.parseDouble(body.kr().replaceAll(",", ".")));
        lettura.setKp(Double.parseDouble(body.kp().replaceAll(",", ".")));
        lettura.setNote(body.note());
        return letturaRepository.save(lettura);
    }

    /**
     * Da dove parte il periodo delle letture di una fattura (vedi periodoLetture).
     */
    public enum OrigineInizio {FATTURA_PRECEDENTE, LETTURA_SWITCH, FINE_MESE_PRECEDENTE}

    /**
     * Periodo delle letture della fattura del mese: estremi inclusi e da dove parte.
     */
    public record PeriodoLetture(LocalDate dal, LocalDate al, OrigineInizio origine) {
    }

    /**
     * Periodo delle letture della fattura del mese, fino all'ultimo giorno del mese. Inizio:
     * <ul>
     *     <li>la lettura finale della fattura del mese precedente;</li>
     *     <li>se non c'è (prima fattura), la lettura di switch (Raccolta = S) se cade nel periodo: è datata il giorno
     *     prima dello switch;</li>
     *     <li>altrimenti la lettura di fine mese precedente (per un cliente nuovo le letture precedenti allo switch
     *     non arrivano, quindi si parte comunque dalla prima lettura disponibile).</li>
     * </ul>
     * La data di switch della fornitura non si usa: è inserita a mano e non è affidabile.
     */
    public PeriodoLetture periodoLetture(Fornitura fornitura, int mese, int anno) {
        LocalDate primoDelMese = LocalDate.of(anno, mese, 1);
        LocalDate mesePrecedente = primoDelMese.minusMonths(1);
        LocalDate to = primoDelMese.withDayOfMonth(primoDelMese.lengthOfMonth());
        LocalDate fineMesePrecedente = primoDelMese.minusDays(1);
        Optional<LocalDate> fatturata = fatturaSingolaRepository
                .findDataUltimaLetturaFatturata(fornitura, mesePrecedente.getMonthValue(), mesePrecedente.getYear());
        if (fatturata.isPresent()) return new PeriodoLetture(fatturata.get(), to, OrigineInizio.FATTURA_PRECEDENTE);
        return letturaRepository
                .findFirstByFornituraAndRaccoltaAndDataLetturaBetweenOrderByDataLetturaDesc(fornitura, Lettura.RACCOLTA_SWITCH, fineMesePrecedente, to)
                .map(l -> new PeriodoLetture(l.getDataLettura(), to, OrigineInizio.LETTURA_SWITCH))
                .orElse(new PeriodoLetture(fineMesePrecedente, to, OrigineInizio.FINE_MESE_PRECEDENTE));
    }

    /**
     * Letture del periodo della fattura del mese (periodoLetture), ordinate per data decrescente.
     */
    public List<Lettura> lettureDelPeriodo(Fornitura fornitura, PeriodoLetture periodo) {
        return letturaRepository.findByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(fornitura, periodo.dal(), periodo.al());
    }

    public List<Lettura> getLetture(Fornitura fornitura, int mese, int anno) {
        List<Lettura> letture = lettureDelPeriodo(fornitura, periodoLetture(fornitura, mese, anno));
        if (letture.size() < 2)
            throw new RuntimeException(fornitura.getId() + " non ci sono abbastanza letture per calcolare il consumo per il mese " + mese + " dell'anno " + anno);
        return letture;
    }

    /**
     * Consumi attivi per fascia degli ultimi mesi di calendario (compreso quello in corso), dalle letture: per ogni
     * mese dall'ultima lettura fino a fine mese precedente all'ultima fino a fine mese (nel mese dello switch dalla
     * prima lettura del mese), con le costanti del contatore come in fattura. I mesi senza letture sufficienti non ci
     * sono, e nemmeno quelli che partirebbero da una lettura di più mesi prima (il consumo sarebbe di più mesi); quelli
     * che non coprono tutto il mese hanno completo = false.
     * Le letture importate dagli excel iniziali hanno ka = 0: per quelle si usa la costante della lettura valida più
     * vicina (la successiva, o la precedente), altrimenti risulterebbero consumi nulli e poi un picco.
     */
    public List<ConsumoMensileDTO> consumiMensili(Fornitura fornitura, int mesi) {
        YearMonth ultimo = YearMonth.now();
        YearMonth primo = ultimo.minusMonths(mesi - 1L);
        List<Lettura> letture = letturaRepository
                .findByFornituraAndDataLetturaBetweenOrderByDataLetturaDesc(fornitura, LocalDate.of(2000, 1, 1), ultimo.atEndOfMonth())
                .stream().sorted(Comparator.comparing(Lettura::getDataLettura)).toList();
        double[] costanti = costantiAttiva(letture);
        List<ConsumoMensileDTO> consumi = new ArrayList<>();
        for (YearMonth ym = primo; !ym.isAfter(ultimo); ym = ym.plusMonths(1)) {
            LocalDate fineMesePrecedente = ym.minusMonths(1).atEndOfMonth();
            LocalDate fineMese = ym.atEndOfMonth();
            int fine = ultimaFinoA(letture, fineMese);
            if (fine < 0 || letture.get(fine).getDataLettura().isBefore(ym.atDay(1))) continue;
            int inizio = ultimaFinoA(letture, fineMesePrecedente);
            if (inizio < 0) {
                // primo mese con letture (switch): si parte dalla prima lettura del mese
                inizio = 0;
            }
            if (fine <= inizio) continue;
            // partenza da prima del mese precedente (mancano letture di fine mese): il consumo sarebbe di più mesi
            if (letture.get(inizio).getDataLettura().isBefore(ym.minusMonths(1).atDay(1))) continue;
            // somma tra letture consecutive, come getConsumi
            double f1 = 0, f2 = 0, f3 = 0;
            for (int i = inizio + 1; i <= fine; i++) {
                Lettura prec = letture.get(i - 1);
                Lettura curr = letture.get(i);
                f1 += nvl(curr.getEaF1()) * costanti[i] - nvl(prec.getEaF1()) * costanti[i - 1];
                f2 += nvl(curr.getEaF2()) * costanti[i] - nvl(prec.getEaF2()) * costanti[i - 1];
                f3 += nvl(curr.getEaF3()) * costanti[i] - nvl(prec.getEaF3()) * costanti[i - 1];
            }
            LocalDate dal = letture.get(inizio).getDataLettura();
            LocalDate al = letture.get(fine).getDataLettura();
            consumi.add(new ConsumoMensileDTO(ym.getMonthValue(), ym.getYear(), f1, f2, f3, f1 + f2 + f3,
                    dal, al, dal.equals(fineMesePrecedente) && al.equals(fineMese), 1, 1));
        }
        return consumi;
    }

    /**
     * Consumi mensili per fascia di più forniture (es. tutti i pod di un cliente) sommati mese per mese
     * (consumiMensili di ognuna). Un mese c'è se almeno un pod ha consumo; è completo solo se tutti i pod lo hanno
     * per l'intero mese. dal/al sono la lettura più vecchia e la più recente usate.
     */
    public List<ConsumoMensileDTO> consumiMensili(List<Fornitura> forniture, int mesi) {
        Map<YearMonth, List<ConsumoMensileDTO>> perMese = new java.util.TreeMap<>();
        for (Fornitura fornitura : forniture) {
            for (ConsumoMensileDTO c : consumiMensili(fornitura, mesi)) {
                perMese.computeIfAbsent(YearMonth.of(c.anno(), c.mese()), k -> new ArrayList<>()).add(c);
            }
        }
        List<ConsumoMensileDTO> somme = new ArrayList<>();
        perMese.forEach((ym, consumi) -> somme.add(new ConsumoMensileDTO(ym.getMonthValue(), ym.getYear(),
                consumi.stream().mapToDouble(ConsumoMensileDTO::f1).sum(),
                consumi.stream().mapToDouble(ConsumoMensileDTO::f2).sum(),
                consumi.stream().mapToDouble(ConsumoMensileDTO::f3).sum(),
                consumi.stream().mapToDouble(ConsumoMensileDTO::totale).sum(),
                consumi.stream().map(ConsumoMensileDTO::dal).min(Comparator.naturalOrder()).orElseThrow(),
                consumi.stream().map(ConsumoMensileDTO::al).max(Comparator.naturalOrder()).orElseThrow(),
                consumi.size() == forniture.size() && consumi.stream().allMatch(ConsumoMensileDTO::completo),
                consumi.size(), forniture.size())));
        return somme;
    }

    /**
     * Costante ka di ogni lettura (lista ordinata per data); dove manca (null o 0) quella della lettura valida
     * successiva più vicina, o della precedente se dopo non ce ne sono; 1 se nessuna lettura ce l'ha.
     */
    private static double[] costantiAttiva(List<Lettura> letture) {
        double[] k = new double[letture.size()];
        double successiva = 0;
        for (int i = letture.size() - 1; i >= 0; i--) {
            Double ka = letture.get(i).getKa();
            if (ka != null && ka != 0) successiva = ka;
            k[i] = successiva;
        }
        double precedente = 1;
        for (int i = 0; i < k.length; i++) {
            if (k[i] == 0) k[i] = precedente;
            else precedente = k[i];
        }
        return k;
    }

    /**
     * Indice dell'ultima lettura (lista ordinata per data crescente) con data fino a quella indicata, -1 se nessuna.
     */
    private static int ultimaFinoA(List<Lettura> letture, LocalDate data) {
        int indice = -1;
        for (int i = 0; i < letture.size() && !letture.get(i).getDataLettura().isAfter(data); i++) indice = i;
        return indice;
    }

    public Map<String, Double> getConsumi(List<Lettura> letture) {
        Map<String, Double> consumi = new HashMap<>();
        if (letture.size() < 2) throw new RuntimeException("Non ci sono abbastanza letture per calcolare il consumo");
        List<Lettura> ordinate = letture.stream()
                .sorted(Comparator.comparing(Lettura::getDataLettura))
                .toList();
        double eaF1 = 0, eaF2 = 0, eaF3 = 0, erF1 = 0, erF2 = 0, erF3 = 0, ercF1 = 0, ercF2 = 0, ercF3 = 0;
        double potMax = 0;
        // somma dei consumi tra letture consecutive (senza cambi contatore equivale a ultima - prima).
        // Le letture vanno moltiplicate per le costanti del contatore: ka attiva, kr reattiva, kp potenza.
        // TODO: gestire le letture di CAMBIO contatore (sommare i consumi fino al cambio e ripartire da zero)
        for (int i = 1; i < ordinate.size(); i++) {
            Lettura prec = ordinate.get(i - 1);
            Lettura curr = ordinate.get(i);
            eaF1 += nvl(curr.getEaF1()) * ka(curr) - nvl(prec.getEaF1()) * ka(prec);
            eaF2 += nvl(curr.getEaF2()) * ka(curr) - nvl(prec.getEaF2()) * ka(prec);
            eaF3 += nvl(curr.getEaF3()) * ka(curr) - nvl(prec.getEaF3()) * ka(prec);
            erF1 += nvl(curr.getErF1()) * kr(curr) - nvl(prec.getErF1()) * kr(prec);
            erF2 += nvl(curr.getErF2()) * kr(curr) - nvl(prec.getErF2()) * kr(prec);
            erF3 += nvl(curr.getErF3()) * kr(curr) - nvl(prec.getErF3()) * kr(prec);
            // reattiva immessa: solo tra letture che hanno il dato (le letture importate prima non lo hanno)
            ercF1 += delta(curr.getErcF1(), prec.getErcF1(), kr(curr), kr(prec));
            ercF2 += delta(curr.getErcF2(), prec.getErcF2(), kr(curr), kr(prec));
            ercF3 += delta(curr.getErcF3(), prec.getErcF3(), kr(curr), kr(prec));
            potMax = Math.max(potMax, kp(curr) * Math.max(nvl(curr.getPotF1()), Math.max(nvl(curr.getPotF2()), nvl(curr.getPotF3()))));
        }
        consumi.put("EaF1", eaF1);
        consumi.put("EaF2", eaF2);
        consumi.put("EaF3", eaF3);
        consumi.put("ErF1", erF1);
        consumi.put("ErF2", erF2);
        consumi.put("ErF3", erF3);
        consumi.put("ErcF1", ercF1);
        consumi.put("ErcF2", ercF2);
        consumi.put("ErcF3", ercF3);
        consumi.put("perditeF1", consumi.get("EaF1") * PERCENTUALE_PERDITE / 100);
        consumi.put("perditeF2", consumi.get("EaF2") * PERCENTUALE_PERDITE / 100);
        consumi.put("perditeF3", consumi.get("EaF3") * PERCENTUALE_PERDITE / 100);
        Double consumoTot = consumi.get("EaF1") + consumi.get("EaF2") + consumi.get("EaF3");
        Double consumoTotP = consumoTot + consumi.get("perditeF1") + consumi.get("perditeF2") + consumi.get("perditeF3");
        Double consumoTotR = consumi.get("ErF1") + consumi.get("ErF2") + consumi.get("ErF3");
        consumi.put("consumoTot", consumoTot);
        consumi.put("consumoTotP", consumoTotP);
        consumi.put("consumoTotR", consumoTotR);
        consumi.put("potMax", potMax);
        if (consumoTotR > 0) {
            Double percentualeReattiva = (consumi.get("consumoTotR") / consumi.get("consumoTot")) * 100;
            consumi.put("percentualeReattiva", percentualeReattiva);
        }
        return consumi;
    }

    /**
     * Consumo totale di energia attiva calcolato solo tra la prima e l'ultima lettura,
     * senza considerare le letture intermedie: serve per verificare il risultato di getConsumi.
     */
    public Double getConsumoTotPrimaUltima(Lettura prima, Lettura ultima) {
        return (nvl(ultima.getEaF1()) * ka(ultima) - nvl(prima.getEaF1()) * ka(prima))
                + (nvl(ultima.getEaF2()) * ka(ultima) - nvl(prima.getEaF2()) * ka(prima))
                + (nvl(ultima.getEaF3()) * ka(ultima) - nvl(prima.getEaF3()) * ka(prima));
    }

    private static double delta(Double corrente, Double precedente, double kCorrente, double kPrecedente) {
        if (corrente == null || precedente == null) return 0;
        return corrente * kCorrente - precedente * kPrecedente;
    }

    private static Double parseDecimale(String valore) {
        return valore != null && !valore.isBlank() ? Double.parseDouble(valore.replaceAll(",", ".")) : null;
    }

    // costanti del contatore: se mancano valgono 1
    private static double ka(Lettura l) {
        return l.getKa() != null ? l.getKa() : 1;
    }

    private static double kr(Lettura l) {
        return l.getKr() != null ? l.getKr() : 1;
    }

    private static double kp(Lettura l) {
        return l.getKp() != null ? l.getKp() : 1;
    }

    private static double nvl(Double valore) {
        return valore != null ? valore : 0;
    }

    public Map<String, Double> getConsumi(Fornitura fornitura, int mese) {
        mese = mese + 1;
        LocalDate date = LocalDate.now().minusMonths(mese);
        List<Lettura> letture = getLetture(fornitura, date.getMonthValue(), date.getYear());
        return getConsumi(letture);
    }

}
