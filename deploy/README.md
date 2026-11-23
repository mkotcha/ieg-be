# Deploy di IEG (questa macchina)

```
internet ── Cloudflare (HTTPS) ──tunnel──▶ cloudflared ──▶ Caddy :8076 (Host: ieg.emmek.org)
                                                             ├─ /api/*, /auth/* → container ieg-be :8080 → Postgres "ieg-2.1"
                                                             └─ /               → ~/.local/share/ieg/www (build di ieg-fe)
```

**Prod segue il ramo `main` su GitHub** (`mkotcha/ieg-be` e `mkotcha/ieg-fe`). Si lavora su `develop` e sui rami
di feature; per pubblicare si porta `develop` in `main` e si fa il push: entro 5 minuti prod è aggiornato.

```
git switch main && git pull --ff-only && git merge develop && git push && git switch develop
```

(oppure merge di una PR su `main` direttamente su GitHub: è lo stesso.)

## Comandi

| cosa | comando |
|---|---|
| deploy subito (senza aspettare il timer) | `systemctl --user start ieg-deploy` |
| ripubblicare anche se `main` non è cambiato | `~/.local/bin/ieg-deploy.sh --force` |
| log dei deploy | `journalctl --user -u ieg-deploy` (`-f` per seguirlo) |
| commit pubblicati | `cat ~/.local/share/ieg/deployed/*` |
| prossimo controllo del timer | `systemctl --user list-timers ieg-deploy.timer` |
| fermare / riattivare il deploy automatico | vedi [sotto](#fermare-il-deploy-automatico) |
| stato dei servizi | `systemctl --user status ieg-be caddy cloudflare-tunnel` |
| log del backend | `podman logs -f ieg-be` (anche `~/dev/ieg-be/volume/log/ieg.log`) |
| riavviare il backend | `systemctl --user restart ieg-be` |

### Fermare il deploy automatico

Il timer è quello che controlla `main` ogni 5 minuti: fermandolo prod resta com'è (backend e sito continuano a
girare), semplicemente i push su `main` non vengono più pubblicati finché non lo si riattiva.

```
# fermarlo fino al prossimo riavvio della macchina (al boot riparte da solo)
systemctl --user stop ieg-deploy.timer

# fermarlo in modo permanente (resta spento anche dopo un riavvio)
systemctl --user disable --now ieg-deploy.timer

# interrompere un deploy in corso (il commit non viene segnato come pubblicato)
systemctl --user stop ieg-deploy.service

# verificare: "inactive" / "disabled" = fermo
systemctl --user is-active ieg-deploy.timer
systemctl --user is-enabled ieg-deploy.timer

# riattivarlo (se nel frattempo main è cambiato, lo pubblica entro 5 minuti)
systemctl --user enable --now ieg-deploy.timer
```

Con il timer fermo si può comunque pubblicare a mano: `systemctl --user start ieg-deploy` (un solo giro).

Interrompere un deploy a metà build del backend lascia in funzione il container di prima; a metà build del frontend
il sito resta com'era (la copia in `www/` si fa solo a build finita). Se lo si interrompe subito dopo il restart di
`ieg-be`, il backend è già quello nuovo ma il commit non risulta pubblicato: al giro successivo viene rifatto.

Per spegnere proprio prod (backend fermo, il sito risponde ma le chiamate alle API falliscono):
`systemctl --user disable --now ieg-deploy.timer && systemctl --user stop ieg-be`. Il quadlet `ieg-be` riparte
comunque al boot: per tenerlo spento anche dopo un riavvio va tolto o commentato `WantedBy=default.target` in
`~/.config/containers/systemd/ieg-be.container`, poi `systemctl --user daemon-reload`.

**Tornare a una versione precedente:** il deploy pubblica sempre la punta di `main`, quindi si fa
`git revert` del commit sbagliato su `main` e push. Il timer pubblica la versione corretta al giro successivo.

## Come funziona il deploy

`ieg-deploy.timer` lancia `ieg-deploy.service` (cioè `~/.local/bin/ieg-deploy.sh`) 2 minuti dopo il boot e poi
ogni 5 minuti. Lo script, per `ieg-be` e `ieg-fe`:

1. fa il fetch di `origin/main` in un clone dedicato in `~/.local/share/ieg/src/<repo>`: **mai da `~/dev`**,
   quindi quello che si fa in sviluppo (rami, build, immagini locali) non tocca prod;
2. se il commit è uguale a quello in `~/.local/share/ieg/deployed/<repo>` non fa niente;
3. altrimenti:
   - **ieg-be**: `podman build` dell'immagine `localhost/ieg-be:latest` (vedi `Dockerfile`: build Maven nel
     container, runtime JRE 21 con `pg_dump` 18 per il backup del db), `systemctl --user restart ieg-be`,
     attesa che risponda sulla 8080;
   - **ieg-fe**: `pnpm install` + `pnpm build` in un container `node:22-slim` (non serve nvm), con le variabili
     `VITE_REACT_APP_*` copiate da `~/.config/ieg/frontend.env`; poi `rsync` di `dist/` in `~/.local/share/ieg/www`.
     La copia avviene solo a build riuscita: se fallisce, il sito resta com'era;
4. segna il commit come pubblicato solo se tutto è andato bene. In caso di errore il servizio risulta fallito
   (vedi `journalctl`) e al giro dopo si riprova.

## File e cartelle

| percorso | cosa |
|---|---|
| `~/dev/ieg-be/deploy/` | sorgenti di script, unit systemd e quadlet (questa cartella) |
| `~/.local/bin/ieg-deploy.sh` | script di deploy installato |
| `~/.config/systemd/user/ieg-deploy.{service,timer}` | unit del deploy automatico |
| `~/.config/containers/systemd/ieg-be.container` | quadlet del backend (rete host, porta 8080) |
| `~/dev/ieg-be/volume/` | dati di prod: `env.properties`, `data/`, `fatture/`, `log/` (cartella di lavoro del container) |
| `~/.config/ieg/frontend.env` | variabili di build del frontend (come `.env.local` di ieg-fe, permessi 600) |
| `~/.local/share/ieg/src/` | cloni di deploy (non modificarli a mano) |
| `~/.local/share/ieg/www/` | frontend pubblicato, servito da Caddy |
| `~/.config/caddy/Caddyfile` | blocco `http://ieg.emmek.org:8076` (Caddy condiviso con daMauro e Suwayomi) |

Dopo una modifica a qualcosa in `deploy/` (script, unit, quadlet) va rilanciato `deploy/install.sh`, che copia
tutto al suo posto e fa `daemon-reload`. Lo script installato non si aggiorna da solo col deploy.
Dopo una modifica al Caddyfile: `systemctl --user restart caddy` (con `admin off` il reload non funziona).

## Sviluppo accanto a prod

Dev e prod girano insieme senza pestarsi i piedi:

| | prod | dev |
|---|---|---|
| backend | container `ieg-be`, porta 8080 | `./mvnw spring-boot:run`, porta 8081 |
| cartella di lavoro | `volume/` | `volume-dev/` |
| database | `ieg-2.1` | `ieg-dev` |
| frontend | `https://ieg.emmek.org` | `pnpm dev` in ieg-fe (proxy Vite → 8081) |

Porta e database vengono da `PORT_SERVER` e `DB_NAME` nell'`env.properties` di ciascuna cartella.
Avviando il backend da IntelliJ, la working directory va impostata su `volume-dev`.

- **Ricopiare `ieg-dev` da prod:**
  ```
  podman exec postgres sh -c 'dropdb -U postgres ieg-dev && createdb -U postgres ieg-dev && pg_dump -U postgres -Fc ieg-2.1 | pg_restore -U postgres -d ieg-dev --no-owner'
  ```
  (fermare prima il backend dev, altrimenti `dropdb` fallisce per le connessioni aperte.)
- **Modifiche allo schema** (`src/main/resources/schema.sql`): vanno applicate a entrambi i database. Su prod
  vanno applicate **prima** del merge su `main`, altrimenti il nuovo backend parte e fallisce con
  "column … does not exist".

## Installazione da zero (nuova macchina)

Prerequisiti già presenti su questa macchina: podman con i quadlet `postgres`, `caddy` e `cloudflare-tunnel`, linger
dell'utente attivo (i servizi partono al boot senza login), `rsync`.

1. Clonare `ieg-be` in `~/dev/ieg-be` e creare `volume/` con `env.properties` (chiavi elencate in `CLAUDE.md`,
   `PORT_SERVER=8080`), `data/`, `fatture/`, `log/`.
2. Creare `~/.config/ieg/frontend.env` (permessi 600) con `VITE_REACT_APP_API_URL=/api`, `VITE_REACT_APP_URL=` e
   `VITE_REACT_APP_PERSIST_KEY=<stringa casuale>`.
3. `~/dev/ieg-be/deploy/install.sh`
4. Caddy: aggiungere il blocco `http://ieg.emmek.org:8076` al Caddyfile e
   `Volume=%h/.local/share/ieg/www:/srv/ieg:ro` a `caddy.container`; poi `systemctl --user daemon-reload` e
   `systemctl --user restart caddy`.
5. Cloudflare: dashboard → **Networks → Tunnels** → tunnel di questa macchina → *Public hostname*
   `ieg.emmek.org` → tipo **HTTP** → URL `localhost:8076`.
6. `systemctl --user enable --now ieg-deploy.timer` e `systemctl --user start ieg-deploy` per il primo deploy.
