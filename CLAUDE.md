# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

IEG (Emmek) back-end: a Spring Boot 3 / Java 21 REST API for managing energy resale/billing for a group of clients (
`Cliente`) and their supply points (`Fornitura`, identified by POD code). It ingests meter readings from distributor XML
flows and Excel files, and computes monthly invoices (`Fattura`) from readings, tariffs, and regulatory charges.

Domain is entirely in Italian — entity, field, and variable names reflect Italian energy-market terminology (see Domain
glossary below). Keep new code consistent with this convention; don't translate existing names to English.

## Build, run, test

Maven wrapper is used; a JDK 21 and a local PostgreSQL instance (db `ieg-2.1`, see `spring.datasource.url`) are
required. Nothing else: the invoice PDFs are generated in pure Java (no Excel, no Python), so the app runs the same on
Linux and Windows.

```
./mvnw spring-boot:run          # run the app (also: mvnw.cmd on Windows)
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw package                  # build jar
```

Runtime files live in `volume/` (gitignored): `env.properties`, `data/` (bootstrap `.xlsx`, `data/uploads`),
`fatture/` (generated PDFs) and `log/`. All code paths below (`data/…`, `fatture/…`, `log/ieg.log`,
`file:env.properties`) are relative to the working directory, which is `volume/`: `spring-boot:run` is configured so in
`pom.xml` (`workingDirectory`), and an IDE run configuration must set it too.

Deployment: `Dockerfile` builds the image (`podman build -t ieg-be .`; it includes `postgresql-client-18` for
`/api/db/dump`), run by the user quadlet `~/.config/containers/systemd/ieg-be.container` (`Network=host`, port 8080,
`volume/` mounted as the working dir `/app/volume`, `DB_DUMP_COMANDO=pg_dump`). Caddy (`~/.config/caddy/Caddyfile`)
serves the frontend build and proxies `/api` and `/auth` to it at `ieg.emmek.org`, exposed via the shared Cloudflare
tunnel. Redeploy: `podman build -t ieg-be . && systemctl --user restart ieg-be`.

Configuration lives in `src/main/resources/application.properties`, which imports `env.properties` (gitignored, not
committed) for secrets: `PG_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_*`, `SENDGRID_*`, `CORS_ALLOWED_ORIGINS`, `ADMIN_*`,
`SECURITY_PASSWORD`, `PORT_SERVER`, and optionally `DB_DUMP_COMANDO` (the pg_dump command used by `GET /api/db/dump`, default `pg_dump`; e.g. `podman exec -e PGPASSWORD postgres pg_dump` when pg_dump only exists in the Postgres container). A local `env.properties` file must exist (with these keys) before the app will
start. `spring.jpa.hibernate.ddl-auto=none` — the schema is **not** generated from entities: it is maintained by hand in
`src/main/resources/schema.sql`. When adding/changing entity columns or tables, update `schema.sql` **and** apply the
matching `ALTER TABLE`/`CREATE TABLE` to the local DB, otherwise Hibernate fails at runtime with "column … does not
exist".

There is no linter config in the repo; formatting follows whatever IntelliJ default was used historically (see `.idea/`,
though that's gitignored-equivalent and shouldn't be relied on).

Test coverage is minimal: a Spring context-load smoke test (`IegApplicationTests`, currently disabled) and two plain
JUnit tests (no Spring/DB): `CalcoloFatturaServiceTest` checks the invoice calculation against the reference invoice
AUEE202611007, `PdfFatturaServiceTest` renders that invoice to `target/fattura-di-esempio.pdf` and checks page count
and text (open the file, or `pdftoppm -png` it, to check the layout after touching the template). Keep them passing
when touching invoice math or the PDF; for everything else verify behavior manually (e.g. via the Swagger UI or manual
requests).

## Architecture

Standard layered Spring MVC structure under `src/main/java/org/emmek/IEG/`:

- `controllers/` — `@RestController`s, one per aggregate (Cliente, Fornitura, Lettura, Fattura, Dispacciamento, Oneri,
  Auth). Thin: validate `@RequestBody` DTOs via `BindingResult` and throw `BadRequestException` on failure, delegate to
  services.
- `services/` — business logic and orchestration. Invoice math lives in `CalcoloFatturaService` (see Invoicing flow).
- `repositories/` — Spring Data JPA repositories, one per entity.
- `entities/` — JPA entities (`@Entity`), Lombok `@Getter/@Setter`.
- `payloads/` — request/response DTOs (records), used instead of exposing entities directly for writes.
- `enums/` — domain enums (`BTA`, `CodiceDistributore`, `Fatturazione`, `TipoContatore`, `TipoLettura`, `TipoOfferta`,
  `TipoPrelievo`, `SezioneFattura`, `GruppoScontrino`) plus a custom `@ValueOfEnum` bean-validation annotation for
  validating incoming enum strings.
- `model/xml/` — JAXB-annotated classes (`FlussoMisure`, `DatiPod`, `DatiPdp`, `Misura`, `MisuraOraria`) mapping the
  distributor's XML meter-reading flow format.
- `model/excel/` — Poiji-annotated model classes (`ClienteModel`, `FornituraModel`, `LetturaModel`, `OffertaModel`,
  `OneriModel`, `DispacciamentoModel`) mapping the bootstrap `.xlsx` import files under `data/`.
- `security/` — stateless JWT auth: `JWTAuthFilter` (reads `Authorization: Bearer`, populates `SecurityContextHolder`),
  `JWTTools` (sign/verify), `SecurityConfig` (CORS, CSRF disabled, `@EnableMethodSecurity` — most write endpoints are
  gated with `@PreAuthorize("hasAuthority('ADMIN')")`).
- `exceptions/` — `@RestControllerAdvice` (`ExceptionsHandler`) mapping domain exceptions (`BadRequestException`,
  `NotFoundException`, `UnauthorizedException`) plus generic `RuntimeException`/framework exceptions to JSON error
  responses; `ExceptionsHandlerFilter` runs before `JWTAuthFilter` in the filter chain to translate exceptions thrown
  from filters.
- `configs/` — `JaxbParser` (shared `JAXBContext` for `FlussoMisure`), `SpringDocConfig` (OpenAPI/Swagger).
- `utils/Runner.java` — `CommandLineRunner` executed on every app startup. It contains the bootstrap steps (create
  ADMIN/USER roles and the admin user from `env.properties`, import clienti/forniture/offerte/letture/oneri/
  dispacciamento from the `.xlsx` files under `data/`), but **all calls in `run()` are currently commented out**: the
  local DB is already populated. Uncomment selectively only when (re)bootstrapping a DB.

### Meter reading ingestion flow

`LettureController.uploadFlussi` → `LetturaService.salvaZip` + `importaFlussi` (background job, see Long-running
jobs): accepts a zip upload, recursively extracts nested
zips, then parses every `*_PDO*` / `*_PNO*` / `*_SNM*` XML file found under `data/uploads` via `JaxbParser`, matching
each `DatiPod` to a `Fornitura` by POD code and upserting the supplier `Lettura` for that POD/date (overwrites the
supplier reading of the same day if there is one, except that a switch reading is never overwritten by a periodic one;
manual readings are never touched, see Manual readings). Flow format spec and
examples: `data/ST_Standard_Tracciati_Misure_v2.0/`. Switch readings come from `SNM`/`SNM2G` flows, are dated
`DataMisura` = the day before the switch date (`DataPrest`; when `DataMisura` is present it always gives the reading
date, even if the `Ea` curve is there), and are stored with `raccolta = "S"`
(`Lettura.isSwitch()`; set from the flow code too, since `Raccolta` in those flows isn't always "S"). This is a heavy, `ADMIN`-only operation — errors per-file/per-record are logged,
counted in the result (`EsitoImportLettureDTO`) and skipped rather than failing the whole batch.

### Manual readings

Supplier readings are never edited or deleted to correct them: `Lettura.attiva` (false = kept but excluded from every
calculation: invoices, `verifica`, `consumiMensili`) and `Lettura.manuale` (entered or corrected by hand). At most one
reading per POD/date is active. `LetturaService.save` (POST) creates a manual active reading and deactivates the others
of that date; `update` (PUT) edits a manual reading in place, while for a supplier reading it deactivates it and puts
the new values in a manual copy; `PUT /api/letture/{id}/attiva?attiva=` toggles one (activating deactivates the others
of the date); `delete` re-activates the only reading left on that date. The import inserts the supplier reading as
inactive when the date already has an active manual one. The form doesn't send `ka`/`kr`/`kp`: they come from the
original (copy) or from the nearest reading of the POD that has them (`completaCostanti`).

### Invoicing flow

`GET /api/clienti/{id}/fattura?mese=&anno=` → `ClienteService.newFattura` → `FatturaService.newfattura` (mese/anno are
the consumption month):

1. Invoice number `AUEE<yyyy><MM of issue month = consumption month + 1><client id, 3 digits>`; if it already exists
   the invoice is overwritten (its `FatturaSingola` rows are deleted first).
2. One `FatturaSingola` per `Fornitura` of the client, with `Oneri` (by BTA/trimestre) and `Dispacciamento`.
3. `LetturaService.getLetture`: readings from the `letturaFinale` date of the previous month's `FatturaSingola` to the
   last day of the month. Without a previous invoice (first invoice) it starts at the switch reading if one falls in
   the period, else at the last day of the previous month (to include the month-end reading). `Fornitura.dataSwitch`
   is entered by hand and unreliable: don't base logic on it. The period rule lives in `periodoLetture`, shared with
   `GET /api/letture/verifica?mese=&anno=` (`VerificaLettureService`): for every fornitura the bulk generation would
   bill it reports missing start/month-end readings, estimated final readings and invoices that can't be computed.
   Only active readings count. `getConsumi` computes consumption over all readings (if one of them is manual, only
   over the first and the last: the supplier's intermediate ones are dropped, so the peak power is the last reading's,
   and `getLetture` returns just those two), multiplied
   by the meter constants (`ka` active, `kr` reactive, `kp` power); losses are 10% per band. The first and last
   readings are stored on the `FatturaSingola` (`letturaIniziale`/`letturaFinale`); `consumoTotVerifica` (last − first)
   is a cross-check that logs a warning when it differs. TODO: meter replacement readings (`TipoLettura.CAMBIO`).
4. `CalcoloFatturaService.calcola` builds every detail line as a `VoceFatturaSingola` (table `voci_fatture_singole`:
   codice, sezione, unità, unitario, quantità, importo, iva, gruppo scontrino) and sets totals, IVA and the "scontrino"
   values (spesa vendita/rete, quote fisse/potenza, prezzi medi) on `FatturaSingola`. Rules: PUN prices or the
   offerta's fixed F1–F3; spread only if `hasSpread` (`Offerta.getSpreadEffettivo`); OCV = `offerta.ocv` €/kWh on
   consumption + losses; power quotas on potenza impegnata if < 16 kW, otherwise on peak power; reactive penalties
   only for BTA6 (F1/F2 per band, plus capacitive "immessa" in F3). The "Rettifica oneri di programmazione" line is
   the fixed monthly `Fornitura.rettificaProgrammazione` (transitional refund of past over-billing, negative; only
   a few PODs have it: the PDF omits the row when it is 0). `AltraPartita` rows of the month (table
   `altre_partite`, e.g. morosità, pratiche; only `attiva` ones, an inactive one is kept but skipped, toggled with
   `PUT /api/altre-partite/{id}/attiva?attiva=`) become `ALTRE_PARTITE` lines: if `esenteIva` (already VAT-inclusive or
   out of scope, e.g. morosità) they stay out of the imponibile and are only added to `totaleDaPagare`; otherwise they
   are imponibile and pay the fornitura's VAT rate.
5. `PdfFatturaService.createFattura` writes `fatture/<anno>/<mese>/<numero> - <ragione sociale>.pdf` (see Invoice
   PDF). `FatturaService.calcolaFattura` computes and saves without generating the file.

`POST /api/fatture/genera?mese=&anno=` (`GenerazioneFattureService`, ADMIN) generates the invoices of all active
clients in parallel (`fatture.generazione.thread`, default 4): each client is computed, saved and rendered to PDF in
its own transaction, an error doesn't stop the others (if only the PDF fails the invoice stays saved and the error is
reported). It runs as a background job whose result is the per-client list of `EsitoFatturaDTO`.

### Long-running jobs

`POST /api/fatture/genera` and `POST /api/letture/flussi` return immediately (202) with a `Lavoro` (`model/Lavoro`):
`LavoroService` runs the work on a background thread and keeps its state in memory (phase, done/total, per-item
errors, final result or fatal error), lost on restart. At most one job per `TipoLavoro` runs at a time (a second start
gets 400). The FE polls `GET /api/lavori/{id}` and, after a page reload, resumes with
`GET /api/lavori/ultimo?tipo=` (204 if none). Services report progress through the `Avanzamento` interface
(`Avanzamento.NESSUNO` when called without a job). Background threads have no request-scoped Hibernate session
(open-in-view): lazy associations must be read inside a transaction (`TransactionTemplate`).

### Supplier invoices and comparison

The wholesale supplier bills IEG monthly with one invoice covering all PODs; its summary csv
(`Fatt_<numero>_<data>.csv[.zip]`: `;`-separated, UTF-8, comma decimals, one row per charge per POD) is imported with
`POST /api/fatture-fornitore/upload` (`FatturaFornitoreService`) into `fatture_fornitore` / `voci_fatture_fornitore`.
Re-importing the same invoice replaces its rows. Each row's competence month is the month of `Inizio Periodo`, so
adjustments (`Tipo Ricalcolo`: reversal of the estimate, articles `*_A`, plus re-billing on actual readings) arriving
in later invoices add up to the month they refer to. Rows dated *after* the invoice's reference month (the month most
of its rows fall in: stamp duty on the issue date, late fees, CMORE morosità events) are moved back to the reference
month on import (`allineaAlMeseDiRiferimento`); earlier months are left alone.

`ConfrontoFatturaService` (`GET /api/fatture/confronto?mese=&anno=` summary, `GET /api/fatture/{numero}/confronto`
detail) maps both sides onto `VoceConfronto`: our lines by `VoceFatturaSingola.codice` (map in the service), the
supplier's with the rules in table `mappature_voci_fornitore` (key = `Cod Tariffa`, or `Nr Articolo` when the tariff is
empty; `*` suffix = prefix; `perdite` for "Perdite ..." rows; `quantita` = the row counts toward the quantity). Unmapped
tariffs end up in `NON_MAPPATA` and are listed by `GET /api/fatture-fornitore/mappature/non-mappate`; new rules go in
via the API and in the seed in `schema.sql`. Pass-through voci (network, system charges, dispatching, reactive) should
match; the margin comes from energy, commercialisation and our capacity mark-up (`MAGGIORAZIONE_CAPACITA`). Excise is
paid by IEG directly (the supplier bills it at 0): neither pass-through nor margin. `COSTI_IEG` (prepayment, late fees,
stamp duty, REMIT) are supplier charges to IEG not tied to a client: excluded per supply, totalled in the monthly
summary.
`GET /api/fatture-fornitore/pod-non-registrati?mese=&anno=` lists PODs that appear in the supplier rows of that
competence month but not in `forniture`,
with the data the summary provides to pre-fill their registration (latest row: `Classe utenza` → BTA, D2/D3/D4 → TD;
powers; `Tipo Utenza` → BT/MT/AT; `Data Apertura Utenza` → switch date; IT001 → EDIST). Client, address and VAT are
not in the summary (its "client" is always IEG).

### Invoice PDF

`PdfFatturaService` fills the Thymeleaf template `src/main/resources/fattura/fattura.html` and converts it to PDF with
openhtmltopdf (PDFBox): page 1 riepilogo, then for every fornitura scontrino dell'energia and allegato di dettaglio.
The template has **no logic or number formatting**: Java puts already formatted strings in maps (`r` riepilogo,
`f.scontrino`, `f.dettaglio`). Things to know when editing it:

- it must stay well-formed XML (closed tags, `&#160;` not `&nbsp;`) and openhtmltopdf only supports CSS 2.1 (tables,
  floats, `@page` margin boxes for "Pagina x di n" and the footer): no flexbox/grid;
- the detail lines come from the saved `VoceFatturaSingola` rows; their grouping and bold group titles are the
  `SEZIONI` structure in the service (codes not listed there are appended to their section; `rettifica` is omitted
  when 0);
- the pie chart is `<object type="grafico/torta" data-valori="...">`, drawn as vectors by `GraficoTorta`;
- the font is `IEGSans-*.ttf`: Carlito (metric-compatible with Calibri, OFL, see `fattura/OFL.txt`) with the GSUB
  table removed, because with ligatures (tt, ti, fi) the PDF text can't be searched or copied.

Until Nov 2026 invoices were produced by filling Excel templates and driving Excel via COM (`ExcelService`,
`script/componiFattura.py`, see git history), which also produced an xlsx: old invoices still have `Fattura.xlsx` and
`GET /api/fatture/{numero}/xlsx` serves it (404 for the new ones; regenerating an invoice clears the field). The old
templates (`template/pagina_*.xlsx`) and the VBA macros the calculation was ported from (`template/*.bas`) are no
longer used by the code; the whole `template/` folder is gitignored and exists only locally.

## Notes / gotchas

- `data/`, `fatture/`, `log/`, `template/`, and all `*.xlsx` files are gitignored — the ones present locally are
  working data, not fixtures to keep in sync with commits.
- About 830 old readings (≤ Jan 2026, from the bootstrap Excel import) have `ka = 0`; `getConsumi` would turn them
  into zero consumption followed by a spike. They are older than any invoice period; the monthly consumption chart
  (`GET /api/forniture/{pod}/consumi`, `LetturaService.consumiMensili`; `GET /api/clienti/{id}/consumi` sums all the
  client's PODs) takes the constant from the nearest valid reading instead.
- IDs for `Cliente` and `Lettura` are often assigned manually (`getNextId()` / imported from Excel) rather than
  DB-generated — be careful with insert logic that assumes auto-increment.
- Numeric fields coming from XML/Excel/DTOs are frequently comma-decimal Italian-formatted strings and require
  `.replaceAll(",", ".")` before `Double.parseDouble` — follow this pattern when adding similar parsing.
