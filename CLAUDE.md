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
required. Invoice generation also needs Windows with Microsoft Excel installed and a `python` on PATH with `xlwings`
and `pywin32` (`python -m pip install xlwings pywin32`): the scripts in `script/` drive Excel via COM to copy template
sheets and export the PDF.

```
./mvnw spring-boot:run          # run the app (also: mvnw.cmd on Windows)
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw package                  # build jar
```

Configuration lives in `src/main/resources/application.properties`, which imports `env.properties` (gitignored, not
committed) for secrets: `PG_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_*`, `SENDGRID_*`, `CORS_ALLOWED_ORIGINS`, `ADMIN_*`,
`SECURITY_PASSWORD`, `PORT_SERVER`. A local `env.properties` file must exist (with these keys) before the app will
start. `spring.jpa.hibernate.ddl-auto=none` — the schema is **not** generated from entities: it is maintained by hand in
`src/main/resources/schema.sql`. When adding/changing entity columns or tables, update `schema.sql` **and** apply the
matching `ALTER TABLE`/`CREATE TABLE` to the local DB, otherwise Hibernate fails at runtime with "column … does not
exist".

There is no linter config in the repo; formatting follows whatever IntelliJ default was used historically (see `.idea/`,
though that's gitignored-equivalent and shouldn't be relied on).

Test coverage is minimal: a Spring context-load smoke test (`IegApplicationTests`) and `CalcoloFatturaServiceTest`, a
plain JUnit test (no Spring/DB) that checks the invoice calculation against the reference invoice AUEE202611007. Keep
that test passing when touching invoice math; for everything else verify behavior manually (e.g. via the Swagger UI or
manual requests).

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
each `DatiPod` to a `Fornitura` by POD code and upserting a `Lettura` for that POD/date (overwrites if one already
exists for the same day, except that a switch reading is never overwritten by a periodic one). Flow format spec and
examples: `data/ST_Standard_Tracciati_Misure_v2.0/`. Switch readings come from `SNM`/`SNM2G` flows, are dated
`DataMisura` = the day before the switch date (`DataPrest`; when `DataMisura` is present it always gives the reading
date, even if the `Ea` curve is there), and are stored with `raccolta = "S"`
(`Lettura.isSwitch()`; set from the flow code too, since `Raccolta` in those flows isn't always "S"). This is a heavy, `ADMIN`-only operation — errors per-file/per-record are logged,
counted in the result (`EsitoImportLettureDTO`) and skipped rather than failing the whole batch.

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
   `getConsumi` computes consumption over all readings, multiplied
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
   a few PODs have it: `ExcelService` hides the row when it is 0). `AltraPartita` rows of the month (table
   `altre_partite`, e.g. morosità, pratiche) become `ALTRE_PARTITE` lines: if `esenteIva` (already VAT-inclusive or
   out of scope, e.g. morosità) they stay out of the imponibile and are only added to `totaleDaPagare`; otherwise they
   are imponibile and pay the fornitura's VAT rate.
5. `ExcelService.createFattura` writes `fatture/<anno>/<mese>/<numero> - <ragione sociale>.xlsx` and `.pdf`: each
   page is filled with POI in a temp copy of its template (sheet renamed first, `preparaFattura`), then
   `script/componiFattura.py` merges the sheets and exports the PDF (`componiFatture`, job list in JSON, one Excel
   instance for all the jobs, restarted if it crashes). The script runs Excel from a thread bound to a hidden Windows
   desktop (`su_desktop_nascosto`): an invisible Excel still shows the "Publishing..." progress window on every
   PDF export, and there it stays off the user's screen. `FatturaService.calcolaFattura` computes and saves without
   generating files.

`POST /api/fatture/genera?mese=&anno=` (`GenerazioneFattureService`, ADMIN) generates the invoices of all active
clients: invoices are computed, saved and their pages filled in parallel (`fatture.generazione.thread`, default 4;
one transaction per client, an error doesn't stop the others), then all xlsx/pdf are composed sequentially by one
`componiFattura.py` run with a single Excel instance (parallel Excel instances slow each other down and crash).
It runs as a background job whose result is the per-client list of `EsitoFatturaDTO`; progress of the composition
phase comes from the "ok …"/"ERRORE …" line the script prints after each invoice.

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
in later invoices add up to the month they refer to.

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

**Templates** (`template/pagina_1.xlsx` riepilogo, `pagina_2.xlsx` scontrino, `pagina_3.xlsx` dettaglio) contain **no
formulas**: Java computes every value and `ExcelService` only writes into named cells. Detail lines are located by the
named cell `voce_<codice>` (column A of the row; columns G–M hold dal, al, unità, unitario, quantità, importo, iva).
The same names exist on several sheets, so lookups must use the name that refers to the target sheet (`findCell`). The
templates are gitignored (`*.xlsx`); previous versions are in `template/bak/`. The VBA macros the Java flow was ported
from are in `template/*.bas`.

## Notes / gotchas

- `data/`, `fatture/`, `log/`, and all `*.xlsx` files are gitignored — the ones present locally are working data, not
  fixtures to keep in sync with commits.
- IDs for `Cliente` and `Lettura` are often assigned manually (`getNextId()` / imported from Excel) rather than
  DB-generated — be careful with insert logic that assumes auto-increment.
- Numeric fields coming from XML/Excel/DTOs are frequently comma-decimal Italian-formatted strings and require
  `.replaceAll(",", ".")` before `Double.parseDouble` — follow this pattern when adding similar parsing.
