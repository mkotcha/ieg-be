package org.emmek.IEG.services;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.FSObjectDrawerFactory;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import lombok.extern.slf4j.Slf4j;
import org.emmek.IEG.entities.*;
import org.emmek.IEG.enums.SezioneFattura;
import org.emmek.IEG.enums.TipoLettura;
import org.emmek.IEG.repositories.FatturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.*;

/**
 * Genera il pdf della fattura, solo in Java (nessun programma esterno): il template
 * {@code src/main/resources/fattura/fattura.html} è compilato con Thymeleaf e convertito in pdf con openhtmltopdf.
 * Tutti i valori sono calcolati da {@link CalcoloFatturaService} e salvati su fattura/fattura singola: qui vengono
 * solo formattati. Pagina 1 riepilogo, poi per ogni fornitura scontrino dell'energia e allegato di dettaglio.
 */
@Service
@Slf4j
public class PdfFatturaService {

    private static final Locale IT = Locale.ITALIAN;
    private static final String EURO = "€";
    private static final String CARTELLA = "fattura/";
    private static final String FONT = "IEG Sans";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MESE_ANNO = DateTimeFormatter.ofPattern("MMM-yy", IT);
    private static final String TIPO_GRAFICO = "grafico/torta";
    // fette del grafico della pagina 1, nell'ordine dei valori
    private static final List<String> COLORI_GRAFICO = List.of("#00B0F0", "#FFC000", "#FF0000", "#A5A5A5", "#92D050");
    private static final List<String> VOCI_GRAFICO = List.of("Spesa per la materia energia",
            "Spesa per trasporto, gestione contatore", "Imposte", "Altre partite", "IVA");
    // righe del dettaglio omesse quando l'importo è 0 (es. la rettifica, che hanno solo alcune forniture)
    private static final Set<String> RIGHE_NASCOSTE_SE_ZERO = Set.of("rettifica");

    /**
     * Gruppo di righe del dettaglio: titolo in grassetto (null = nessuno) e codici delle voci, nell'ordine.
     */
    private record Gruppo(String titolo, List<String> codici) {
    }

    private record Sezione(SezioneFattura sezione, String titolo, String nomeTotale, List<Gruppo> gruppi) {
    }

    // struttura dell'allegato di dettaglio; le voci non elencate finiscono in coda alla loro sezione
    private static final List<Sezione> SEZIONI = List.of(
            new Sezione(SezioneFattura.MATERIA, "SPESA PER LA MATERIA ENERGIA", "SPESA PER LA MATERIA ENERGIA", List.of(
                    new Gruppo("Quota Fissa", List.of("aggregazione")),
                    new Gruppo("Quota Energia F1", List.of("energia_f1", "perdite_f1")),
                    new Gruppo("Quota Energia F2", List.of("energia_f2", "perdite_f2")),
                    new Gruppo("Quota Energia F3", List.of("energia_f3", "perdite_f3")),
                    new Gruppo(null, List.of("spread")),
                    new Gruppo("Dispacciamento", List.of("dispacciamento", "perdite_dispacciamento")),
                    new Gruppo(null, List.of("commercializzazione")),
                    new Gruppo(null, List.of("ocv")),
                    new Gruppo(null, List.of("rettifica")))),
            new Sezione(SezioneFattura.TRASPORTO, "TRASPORTO E LA GESTIONE DEL CONTATORE", "TRASPORTO E GESTIONE DEL CONTATORE", List.of(
                    new Gruppo("Quota Energia", List.of("maggiorazione_capacita", "perdite_maggiorazione_capacita",
                            "qe_tud", "qe_uc3", "qe_uc6", "trasmissione")),
                    new Gruppo(null, List.of("reattiva_33", "reattiva_75")),
                    new Gruppo(null, List.of("reattiva_immessa")),
                    new Gruppo("Quota Fissa", List.of("qf_tud", "qf_mis", "qf_uc6", "qf_dbt")),
                    new Gruppo("Quota Potenza", List.of("qp_tdm")))),
            new Sezione(SezioneFattura.ONERI, "ONERI DI SISTEMA", "SPESA ONERI DI SISTEMA", List.of(
                    new Gruppo("ASOS Spesa oneri di sistema", List.of("qe_asos", "qf_asos", "qp_asos")),
                    new Gruppo("ARIM Rimanenti oneri generali", List.of("qe_arim", "qf_arim", "qp_arim")))),
            new Sezione(SezioneFattura.IMPOSTE, "IMPOSTE", "IMPOSTE", List.of(
                    new Gruppo(null, List.of("addizionale_erariale")))));

    static {
        // openhtmltopdf scrive su java.util.logging tempi e font caricati a ogni pdf: restano solo gli avvisi
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, java.util.logging.Level.WARNING));
    }

    @Autowired
    FatturaRepository fatturaRepository;

    private final TemplateEngine templateEngine = templateEngine();
    private final String logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(risorsa("logo.png"));
    // IEG Sans è Carlito (stesse metriche di Calibri, il font delle fatture fatte con Excel) senza la tabella GSUB:
    // con le legature (tt, ti, fi) il testo del pdf non si può cercare né copiare. Licenza in fattura/OFL.txt
    private final byte[] fontNormale = risorsa("IEGSans-Regular.ttf");
    private final byte[] fontGrassetto = risorsa("IEGSans-Bold.ttf");

    // ------------------------------------------------------------------
    //  generazione
    // ------------------------------------------------------------------

    /**
     * Genera il pdf della fattura e lo registra sulla fattura.
     */
    public void createFattura(Fattura fattura) throws IOException {
        fattura.setPdf(generaPdf(fattura));
        // le fatture generate prima avevano anche l'xlsx, che dopo il ricalcolo non corrisponde più
        fattura.setXlsx(null);
        fatturaRepository.save(fattura);
    }

    /**
     * Scrive il pdf in {@code fatture/<anno>/<mese>/<numero> - <ragione sociale>.pdf}. Usa solo Java e non ha stato:
     * può essere eseguito in parallelo per più fatture. Le associazioni lazy della fattura devono essere leggibili
     * (richiesta http o transazione aperta).
     *
     * @return il percorso del file
     */
    public String generaPdf(Fattura fattura) throws IOException {
        // nome file come nelle fatture di esempio: "<numero> - <ragione sociale>" senza caratteri non validi
        String nomeFile = (fattura.getNumeroFattura() + " - " + fattura.getCliente().getRagioneSociale())
                .replaceAll("[<>|/*\\\\?\":.]", "").trim();
        Path pdf = Path.of("fatture", String.valueOf(fattura.getAnno()), String.valueOf(fattura.getMese()), nomeFile + ".pdf");
        Files.createDirectories(pdf.getParent());
        byte[] contenuto = pdf(fattura);
        // file temporaneo e spostamento: un errore di scrittura non lascia un pdf troncato al posto di quello buono
        Path temporaneo = Files.createTempFile(pdf.getParent(), "fattura_", ".tmp");
        try {
            Files.write(temporaneo, contenuto);
            Files.move(temporaneo, pdf, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaneo);
        }
        // separatore "/" anche su Windows, come i percorsi già salvati
        return pdf.toString().replace('\\', '/');
    }

    /**
     * Pdf della fattura in memoria.
     */
    public byte[] pdf(Fattura fattura) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFont(() -> new ByteArrayInputStream(fontNormale), FONT, 400, FontStyle.NORMAL, true);
        builder.useFont(() -> new ByteArrayInputStream(fontGrassetto), FONT, 700, FontStyle.NORMAL, true);
        builder.useObjectDrawerFactory(new GraficoTorta());
        builder.withHtmlContent(html(fattura), null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    /**
     * Html della fattura: il template compilato con i valori già formattati.
     */
    String html(Fattura fattura) {
        Context context = new Context(IT);
        context.setVariable("logo", logo);
        context.setVariable("r", riepilogo(fattura));
        List<Map<String, Object>> forniture = new ArrayList<>();
        for (FatturaSingola fs : fattura.getFattureSingole()) {
            forniture.add(Map.of("scontrino", scontrino(fs), "dettaglio", dettaglio(fs)));
        }
        context.setVariable("forniture", forniture);
        return templateEngine.process("fattura", context);
    }

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver(PdfFatturaService.class.getClassLoader());
        resolver.setPrefix(CARTELLA);
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static byte[] risorsa(String nome) {
        try (InputStream in = PdfFatturaService.class.getClassLoader().getResourceAsStream(CARTELLA + nome)) {
            if (in == null) throw new IllegalStateException("Risorsa " + CARTELLA + nome + " non trovata");
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere la risorsa " + CARTELLA + nome, e);
        }
    }

    // ------------------------------------------------------------------
    //  pagina 1: riepilogo
    // ------------------------------------------------------------------

    private Map<String, Object> riepilogo(Fattura fattura) {
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

        Map<String, Object> r = new HashMap<>();
        r.put("numero", fattura.getNumeroFattura());
        r.put("data", data(dataFattura));
        r.put("periodo", "Fattura di " + nomeMese(fattura.getMese()) + " " + fattura.getAnno());
        r.put("scadenza", "Scadenza " + YearMonth.from(dataFattura).atEndOfMonth().getDayOfMonth() + " "
                + nomeMese(dataFattura.getMonthValue()) + " " + dataFattura.getYear());
        r.put("ragioneSociale", cliente.getRagioneSociale());
        r.put("indirizzo1", cliente.getIndirizzo());
        r.put("indirizzo2", String.format("%05d", cliente.getCap()) + " " + cliente.getComune() + " (" + cliente.getProvincia() + ")");
        r.put("partitaIva", cliente.getPIva());
        r.put("codiceCliente", codiceCliente(cliente));
        // i pagamenti non sono registrati: il testo è sempre questo, riferito alla data di emissione
        r.put("situazionePagamenti", "Le fatture scadute alla data del " + data(dataFattura) + " risultano pagate. Grazie");
        // sintesi degli importi
        r.put("materia", euro(materia));
        r.put("oneri", euro(trasportoOneri));
        r.put("accise", euro(accise));
        r.put("partite", euro(partite));
        r.put("iva", euro(iva));
        r.put("totale", euro(totale));
        r.put("consumo", formato(consumo, "#,##0.000") + " kWh");
        r.put("costoMedioMateria", consumo > 1 ? euro(materia / consumo) : "");
        r.put("costoMedioBolletta", consumo > 1 ? euro(totale / consumo) : "");
        // riepilogo iva: l'imponibile comprende le altre partite soggette a iva
        r.put("aliquota", percentuale(nvl(fattureSingole.get(0).getFornitura().getIva()) / 100));
        r.put("imponibile", euro(nvl(fattura.getTotaleImponibile())));
        r.put("importoIva", euro(iva));
        // grafico
        r.put("grafico", materia + ";" + trasportoOneri + ";" + accise + ";" + partite + ";" + iva);
        List<Map<String, String>> legenda = new ArrayList<>();
        for (int i = 0; i < VOCI_GRAFICO.size(); i++) {
            legenda.add(Map.of("nome", VOCI_GRAFICO.get(i), "colore", COLORI_GRAFICO.get(i)));
        }
        r.put("legenda", legenda);
        return r;
    }

    // ------------------------------------------------------------------
    //  pagina 2: scontrino dell'energia (una per fornitura)
    // ------------------------------------------------------------------

    private Map<String, Object> scontrino(FatturaSingola fs) {
        Fornitura fornitura = fs.getFornitura();
        Fattura fattura = fs.getFattura();
        Offerta offerta = fornitura.getOfferta();
        Lettura lettura = fs.getLetturaFinale();
        Lettura letturaOld = fs.getLetturaIniziale();
        double kp = lettura.getKp() != null ? lettura.getKp() : 1;
        double consumo = nvl(fs.getConsumoTot());
        double spesaFissa = nvl(fs.getSpesaFissaVendita()) + nvl(fs.getSpesaFissaRete());
        // imponibile + iva: comprende anche le voci che non confluiscono nelle righe dello scontrino
        double totaleBolletta = nvl(fs.getTotaleDaPagare()) - nvl(fs.getTotalePartite());
        String alMese = " " + EURO + "/mese";

        Map<String, Object> s = new HashMap<>();
        s.put("pod", fornitura.getId());
        s.put("indirizzo", testo(fornitura.getIndirizzo()) + ", " + String.format("%05d", fornitura.getCap()) + " "
                + testo(fornitura.getComune()) + " (" + testo(fornitura.getProvincia()) + ")");
        s.put("potenzaImpegnata", potenza(fornitura.getPotenzaImpegnata()) + " kW");
        // quota per consumi
        s.put("consumoNetto", formato(consumo, "#,##0.000") + " kWh");
        s.put("spesaConsumi", importo(nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaRete())));
        s.put("spesaMedia", prezzoMedio(fs.getSpesaMedia(), "/kWh"));
        s.put("spesaVendita", importo(fs.getSpesaVendita()));
        s.put("spesaVenditaMedia", prezzoMedio(fs.getSpesaVenditaMedia(), "/kWh"));
        s.put("spesaRete", importo(fs.getSpesaRete()));
        s.put("spesaReteMedia", prezzoMedio(fs.getSpesaReteMedia(), "/kWh"));
        // quota fissa (1 mese)
        s.put("spesaFissa", importo(spesaFissa));
        s.put("spesaFissaMedia", importo(spesaFissa) + alMese);
        s.put("spesaFissaVendita", importo(fs.getSpesaFissaVendita()));
        s.put("spesaFissaVenditaMedia", importo(fs.getSpesaFissaVendita()) + alMese);
        s.put("spesaFissaRete", importo(fs.getSpesaFissaRete()));
        s.put("spesaFissaReteMedia", importo(fs.getSpesaFissaRete()) + alMese);
        // quota potenza
        s.put("piccoPotenza", potenza(fs.getPotenzaOneri()) + " kW per 1 mese");
        s.put("spesaPotenza", importo(fs.getSpesaPotenzaRete()));
        s.put("spesaPotenzaMedia", fs.getSpesaPotenzaReteMedia() != null
                ? importo(fs.getSpesaPotenzaReteMedia()) + " " + EURO + "/kW/mese" : "");
        // accise, iva e totali
        s.put("acciseIva", importo(nvl(fs.getTotaleImposte()) + nvl(fs.getTotaleIva())));
        s.put("totaleBolletta", importo(totaleBolletta));
        s.put("totaleDaPagare", importo(fs.getTotaleDaPagare()));
        s.put("partite", altrePartite(fs).stream()
                .map(v -> Map.of("descrizione", testo(v.getDescrizione()), "importo", importo(v.getImporto())))
                .toList());
        // box dell'offerta
        s.put("nomeOfferta", offerta.getNome());
        s.put("codiceOfferta", offerta.getCodice());
        s.put("tipologiaOfferta", offerta.isPun() ? "Offerta a Prezzo Variabile" : "Offerta a prezzo fisso");
        s.put("tipologiaPrezzo", offerta.isPun() ? "Per fasce orarie di prezzo" : "Monoraria");
        s.put("formula", offerta.isPun() ? "Prezzo indicato PUN + Spread" : "Prezzo indicato");
        s.put("dataInizioOfferta", data(offerta.getDataInizio()));
        s.put("dataFineOfferta", data(offerta.getDataFine()));
        s.put("totaleOfferta", formato(nvl(fs.getSpesaVendita()) + nvl(fs.getSpesaFissaVendita()), "#,##0.0000") + " " + EURO);
        s.put("quotaConsumi", "di cui spesa per la quota consumi " + importo(fs.getSpesaVendita()) + " " + EURO);
        s.put("quotaFissa", "di cui spesa per la quota fissa " + importo(fs.getSpesaFissaVendita()) + " " + EURO);
        s.put("valoriFormula", String.format("%02d/%d: F1 %s %s/kWh -  F2 %s %s/kWh -  F3 %s %s/kWh",
                fattura.getMese(), fattura.getAnno(), numero(fs.getPrezzoF1()), EURO, numero(fs.getPrezzoF2()), EURO,
                numero(fs.getPrezzoF3()), EURO));
        // consumi fatturati
        String mese = LocalDate.of(fattura.getAnno(), fattura.getMese(), 1).format(MESE_ANNO);
        s.put("consumi", List.of(
                consumi(mese, "F1", fs.getConsumoF1(), fs.getConsumoF1r(), lettura.getPotF1(), kp),
                consumi(mese, "F2", fs.getConsumoF2(), fs.getConsumoF2r(), lettura.getPotF2(), kp),
                consumi(mese, "F3", fs.getConsumoF3(), fs.getConsumoF3r(), lettura.getPotF3(), kp)));
        s.put("consumoAttivaTot", decimali3(fs.getConsumoTot()));
        s.put("consumoReattivaTot", decimali3(fs.getConsumoTotR()));
        // letture del contatore
        s.put("letture", List.of(lettura(letturaOld), lettura(lettura)));
        return s;
    }

    private static Map<String, String> consumi(String mese, String fascia, Double attiva, Double reattiva,
                                               Double potenza, double kp) {
        return Map.of("mese", mese, "fascia", fascia, "attiva", decimali3(attiva), "reattiva", decimali3(reattiva),
                "potenza", formato(nvl(potenza) * kp, "#,##0.###"));
    }

    private static Map<String, String> lettura(Lettura lettura) {
        return Map.of("data", data(lettura.getDataLettura()), "tipo", tipoLettura(lettura.getTipoLettura()),
                "f1", decimali3(lettura.getEaF1()), "f2", decimali3(lettura.getEaF2()),
                "f3", decimali3(lettura.getEaF3()), "k", numero(lettura.getKa()));
    }

    // ------------------------------------------------------------------
    //  pagina 3: allegato di dettaglio (una per fornitura)
    // ------------------------------------------------------------------

    private Map<String, Object> dettaglio(FatturaSingola fs) {
        Fornitura fornitura = fs.getFornitura();
        Map<String, VoceFatturaSingola> voci = new LinkedHashMap<>();
        for (VoceFatturaSingola voce : fs.getVoci()) {
            if (voce.getSezione() == SezioneFattura.ALTRE_PARTITE) continue;
            if (RIGHE_NASCOSTE_SE_ZERO.contains(voce.getCodice()) && nvl(voce.getImporto()) == 0) continue;
            voci.put(voce.getCodice(), voce);
        }

        List<Map<String, Object>> sezioni = new ArrayList<>();
        for (Sezione sezione : SEZIONI) {
            boolean scaglioni = sezione.sezione() == SezioneFattura.IMPOSTE;
            List<Map<String, Object>> gruppi = new ArrayList<>();
            for (Gruppo gruppo : sezione.gruppi()) {
                List<Map<String, String>> righe = gruppo.codici().stream()
                        .map(voci::remove)
                        .filter(Objects::nonNull)
                        .map(voce -> riga(voce, scaglioni))
                        .toList();
                if (!righe.isEmpty()) gruppi.add(gruppo(gruppo.titolo(), righe));
            }
            // voci aggiunte al calcolo ma non alla struttura: in coda alla sezione, per non perderle
            for (VoceFatturaSingola voce : List.copyOf(voci.values())) {
                if (voce.getSezione() != sezione.sezione()) continue;
                gruppi.add(gruppo(null, List.of(riga(voce, scaglioni))));
                voci.remove(voce.getCodice());
            }
            Map<String, Object> s = new HashMap<>();
            s.put("titolo", sezione.titolo());
            s.put("nomeTotale", sezione.nomeTotale());
            s.put("scaglioni", scaglioni);
            s.put("gruppi", gruppi);
            s.put("totale", euro(totale(fs, sezione.sezione())));
            sezioni.add(s);
        }

        Map<String, Object> d = new HashMap<>();
        d.put("codiceCliente", codiceCliente(fornitura.getCliente()));
        d.put("pod", fornitura.getId());
        d.put("tipoContatore", fornitura.getTipoContatore() != null ? capitalize(fornitura.getTipoContatore().name()) : "");
        d.put("potenzaDisponibile", potenza(fornitura.getPotenzaDisponibile()));
        d.put("potenzaImpegnata", potenza(fs.getPotenzaOneri()));
        d.put("sezioni", sezioni);
        d.put("partite", altrePartite(fs).stream()
                .map(v -> Map.of("descrizione", testo(v.getDescrizione()), "importo", euro(nvl(v.getImporto())),
                        "iva", percentuale(nvl(v.getIva()))))
                .toList());
        d.put("aliquota", percentuale(nvl(fornitura.getIva()) / 100));
        d.put("imponibile", euro(nvl(fs.getTotaleImponibile())));
        d.put("totaleIva", euro(nvl(fs.getTotaleIva())));
        return d;
    }

    private static Map<String, Object> gruppo(String titolo, List<Map<String, String>> righe) {
        Map<String, Object> gruppo = new HashMap<>();
        gruppo.put("titolo", titolo);
        gruppo.put("righe", righe);
        return gruppo;
    }

    private static Map<String, String> riga(VoceFatturaSingola voce, boolean scaglioni) {
        Map<String, String> riga = new HashMap<>();
        riga.put("descrizione", testo(voce.getDescrizione()));
        riga.put("dal", data(voce.getDal()));
        riga.put("al", data(voce.getAl()));
        riga.put("unita", testo(voce.getUnita()));
        riga.put("unitario", voce.getUnitario() != null ? formato(voce.getUnitario(), "#,##0.########") : "");
        riga.put("quantita", quantita(voce.getQuantita()));
        riga.put("importo", euro(nvl(voce.getImporto())));
        riga.put("iva", percentuale(nvl(voce.getIva())));
        // scaglione di consumo dell'accisa
        riga.put("da", scaglioni ? "0" : "");
        riga.put("a", scaglioni ? "200.000" : "");
        return riga;
    }

    private static double totale(FatturaSingola fs, SezioneFattura sezione) {
        return switch (sezione) {
            case MATERIA -> nvl(fs.getTotaleMateria());
            case TRASPORTO -> nvl(fs.getTotaleTrasporto());
            case ONERI -> nvl(fs.getTotaleOneri());
            case IMPOSTE -> nvl(fs.getTotaleImposte());
            case ALTRE_PARTITE -> nvl(fs.getTotalePartite());
        };
    }

    private static List<VoceFatturaSingola> altrePartite(FatturaSingola fs) {
        return fs.getVoci().stream().filter(v -> v.getSezione() == SezioneFattura.ALTRE_PARTITE).toList();
    }

    // ------------------------------------------------------------------
    //  grafico a torta della pagina 1
    // ------------------------------------------------------------------

    /**
     * Disegna in vettoriale l'elemento {@code <object type="grafico/torta" data-valori="v1;v2;...">}: una fetta per
     * ogni valore positivo, con i colori di COLORI_GRAFICO nell'ordine dei valori.
     */
    private static class GraficoTorta implements FSObjectDrawerFactory {
        @Override
        public FSObjectDrawer createDrawer(Element e) {
            if (!isReplacedObject(e)) return null;
            return (elemento, x, y, larghezza, altezza, outputDevice, ctx, dotsPerPixel) -> {
                double[] valori = Arrays.stream(elemento.getAttribute("data-valori").split(";"))
                        .filter(v -> !v.isBlank())
                        .mapToDouble(v -> Math.max(0, Double.parseDouble(v)))
                        .toArray();
                double somma = Arrays.stream(valori).sum();
                float w = (float) larghezza / dotsPerPixel;
                float h = (float) altezza / dotsPerPixel;
                outputDevice.drawWithGraphics((float) x, (float) y, w, h, g -> {
                    if (somma <= 0) return;
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double diametro = Math.min(w, h) - 2;
                    double sx = (w - diametro) / 2;
                    double su = (h - diametro) / 2;
                    // dalle ore 12 in senso orario, come il grafico di Excel
                    double inizio = 90;
                    g.setStroke(new BasicStroke(0.75f));
                    for (int i = 0; i < valori.length; i++) {
                        if (valori[i] <= 0) continue;
                        double angolo = 360 * valori[i] / somma;
                        Shape fetta = new Arc2D.Double(sx, su, diametro, diametro, inizio, -angolo,
                                angolo >= 360 ? Arc2D.CHORD : Arc2D.PIE);
                        g.setColor(Color.decode(COLORI_GRAFICO.get(i % COLORI_GRAFICO.size())));
                        g.fill(fetta);
                        g.setColor(Color.WHITE);
                        g.draw(fetta);
                        inizio -= angolo;
                    }
                });
                return null;
            };
        }

        @Override
        public boolean isReplacedObject(Element e) {
            return "object".equals(e.getNodeName()) && TIPO_GRAFICO.equals(e.getAttribute("type"));
        }
    }

    // ------------------------------------------------------------------
    //  formati
    // ------------------------------------------------------------------

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

    private static String data(LocalDate data) {
        return data != null ? data.format(DATA) : "";
    }

    /**
     * Importo con il simbolo dell'euro (es. "€ 1.234,56").
     */
    private static String euro(double valore) {
        return EURO + " " + formato(valore, "#,##0.00");
    }

    /**
     * Importo senza simbolo (es. "1.234,56"); 0 se manca.
     */
    private static String importo(Double valore) {
        return formato(nvl(valore), "#,##0.00");
    }

    private static String decimali3(Double valore) {
        return valore != null ? formato(valore, "#,##0.000") : "";
    }

    /**
     * Quantità di una riga del dettaglio: intera se lo è (1 PdP), altrimenti con 3 decimali (kWh, kW).
     */
    private static String quantita(Double valore) {
        if (valore == null) return "";
        return formato(valore, valore == Math.rint(valore) ? "#,##0" : "#,##0.000");
    }

    private static String percentuale(double frazione) {
        return formato(frazione * 100, "0.##") + "%";
    }

    /**
     * Numero in formato italiano con il pattern indicato (es. "#,##0.000" -> 1.234,567).
     */
    private static String formato(double valore, String pattern) {
        // evita "-0,00" per gli importi negativi che si arrotondano a zero
        String testo = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(IT)).format(valore);
        return testo.startsWith("-") && testo.chars().noneMatch(c -> c >= '1' && c <= '9') ? testo.substring(1) : testo;
    }

    /**
     * Numero in formato italiano senza zeri finali (es. 28.0 -> "28", 0.238375 -> "0,238375").
     */
    private static String numero(Double valore) {
        if (valore == null) return "";
        return BigDecimal.valueOf(valore).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /**
     * Potenza in kW con al massimo 3 decimali, senza zeri finali (es. 28.799999999999997 -> "28,8", 70.0 -> "70"):
     * il picco prelevato è un prodotto di double e porta con sé l'errore di arrotondamento.
     */
    private static String potenza(Double valore) {
        return valore != null ? formato(valore, "#,##0.###") : "";
    }

    private static String testo(String valore) {
        return valore != null ? valore : "";
    }

    private static double nvl(Double valore) {
        return valore != null ? valore : 0;
    }
}
