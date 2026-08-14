# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

IEG (Emmek) back-end: a Spring Boot 3 / Java 17 REST API for managing energy resale/billing for a group of clients (
`Cliente`) and their supply points (`Fornitura`, identified by POD code). It ingests meter readings from distributor XML
flows and Excel files, and computes monthly invoices (`Fattura`) from readings, tariffs, and regulatory charges.

Domain is entirely in Italian — entity, field, and variable names reflect Italian energy-market terminology (see Domain
glossary below). Keep new code consistent with this convention; don't translate existing names to English.

## Build, run, test

Maven wrapper is used; a JDK 17 and a local PostgreSQL instance (db `ieg`) are required.

```
./mvnw spring-boot:run          # run the app (also: mvnw.cmd on Windows)
./mvnw test                     # run all tests
./mvnw test -Dtest=ClassName    # run a single test class
./mvnw package                  # build jar
```

Configuration lives in `src/main/resources/application.properties`, which imports `env.properties` (gitignored, not
committed) for secrets: `PG_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_*`, `SENDGRID_*`, `CORS_ALLOWED_ORIGINS`, `ADMIN_*`,
`SECURITY_PASSWORD`, `PORT_SERVER`. A local `env.properties` file must exist (with these keys) before the app will
start. `spring.jpa.hibernate.ddl-auto=update` — schema evolves automatically from entities against the local Postgres
DB.

There is no linter config in the repo; formatting follows whatever IntelliJ default was used historically (see `.idea/`,
though that's gitignored-equivalent and shouldn't be relied on).

Test coverage is minimal — only a Spring context-load smoke test exists (`IegApplicationTests`). Don't assume a broader
test suite exists when reasoning about regressions; verify behavior manually (e.g. via the Swagger UI or manual
requests) when changing service logic.

## Architecture

Standard layered Spring MVC structure under `src/main/java/org/emmek/IEG/`:

- `controllers/` — `@RestController`s, one per aggregate (Cliente, Fornitura, Lettura, Fattura, Dispacciamento, Oneri,
  Auth). Thin: validate `@RequestBody` DTOs via `BindingResult` and throw `BadRequestException` on failure, delegate to
  services.
- `services/` — business logic and orchestration. Most invoice math lives in `FatturaService`.
- `repositories/` — Spring Data JPA repositories, one per entity.
- `entities/` — JPA entities (`@Entity`), Lombok `@Getter/@Setter`.
- `payloads/` — request/response DTOs (records), used instead of exposing entities directly for writes.
- `enums/` — domain enums (`BTA`, `CodiceDistributore`, `Fatturazione`, `TipoContatore`, `TipoLettura`, `TipoPrelievo`)
  plus a custom `@ValueOfEnum` bean-validation annotation for validating incoming enum strings.
- `helpers/xml/` — JAXB-annotated classes (`FlussoMisure`, `DatiPod`, `DatiPdp`, `Misura`, `MisuraOraria`) mapping the
  distributor's XML meter-reading flow format.
- `helpers/excel/` — Poiji-annotated model classes (`ClienteModel`, `FornituraModel`, `LetturaModel`) mapping the
  bootstrap `.xlsx` import files under `data/`.
- `security/` — stateless JWT auth: `JWTAuthFilter` (reads `Authorization: Bearer`, populates `SecurityContextHolder`),
  `JWTTools` (sign/verify), `SecurityConfig` (CORS, CSRF disabled, `@EnableMethodSecurity` — most write endpoints are
  gated with `@PreAuthorize("hasAuthority('ADMIN')")`).
- `exceptions/` — `@RestControllerAdvice` (`ExceptionsHandler`) mapping domain exceptions (`BadRequestException`,
  `NotFoundException`, `UnauthorizedException`) plus generic `RuntimeException`/framework exceptions to JSON error
  responses; `ExceptionsHandlerFilter` runs before `JWTAuthFilter` in the filter chain to translate exceptions thrown
  from filters.
- `configs/` — `JaxbParser` (shared `JAXBContext` for `FlussoMisure`), `SpringDocConfig` (OpenAPI/Swagger).
- `utils/Runner.java` — `CommandLineRunner` executed on every app startup: creates ADMIN/USER roles, creates the admin
  user from `env.properties`, and bulk-imports `data/clienti.xlsx`, `data/forniture.xlsx`, `data/letture.xlsx` into the
  DB (idempotent — skips existing rows, but runs on every boot in dev). Be aware of this when starting the app locally
  against a populated DB.

### Meter reading ingestion flow

`LettureController.uploadFlussi` → `LetturaService.uploadFlussi`: accepts a zip upload, recursively extracts nested
zips, then parses every `*_PDO*` / `*_PNO*` / `*_SNM2G*` XML file found under `data/uploads` via `JaxbParser`, matching
each `DatiPod` to a `Fornitura` by POD code and upserting a `Lettura` for that POD/date (overwrites if one already
exists for the same day). This is a heavy, `ADMIN`-only, synchronous operation — errors per-file/per-record are logged
and skipped rather than failing the whole batch.

### Invoicing flow

## Notes / gotchas

- `data/`, `fatture/`, `log/`, and all `*.xlsx` files are gitignored — the ones present locally are working data, not
  fixtures to keep in sync with commits.
- IDs for `Cliente` and `Lettura` are often assigned manually (`getNextId()` / imported from Excel) rather than
  DB-generated — be careful with insert logic that assumes auto-increment.
- Numeric fields coming from XML/Excel/DTOs are frequently comma-decimal Italian-formatted strings and require
  `.replaceAll(",", ".")` before `Double.parseDouble` — follow this pattern when adding similar parsing.
