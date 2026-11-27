# Deploy di IEG

Due ambienti, su due macchine, ciascuno col suo tunnel Cloudflare:

| | produzione | test |
|---|---|---|
| indirizzo | `https://ieg.emmek.org` | `https://ieg-test.emmek.org` |
| macchina | VPS OVH `159.173.145.129` (`ssh kutch@159.173.145.129`) | PC di sviluppo |
| tunnel Cloudflare | `vps-ovh` | `t14s` |
| pubblica il ramo | `main` | `develop` |
| dati | `ieg-2.1` e `~/dev/ieg-be/volume` sul server | copia del 27/11/2026 sul PC |
| backup | ogni notte, copiati anche sul PC (vedi [Backup](#backup)) | no |

```
internet ── Cloudflare (HTTPS) ──tunnel──▶ cloudflared ──▶ Caddy 127.0.0.1:8076 (Host: ieg.emmek.org / ieg-test.emmek.org)
                                                             ├─ /api/*, /auth/* → container ieg-be :8080 → Postgres "ieg-2.1"
                                                             └─ /               → ~/.local/share/ieg/www (build di ieg-fe)
```

Su entrambe le macchine gira lo stesso meccanismo (`ieg-deploy.sh`, sotto); cambia solo il ramo. Si lavora su
`develop` e sui rami di feature: il merge in `develop` va su **test** entro 5 minuti; quando è a posto si porta
`develop` in `main` e va in **produzione** entro 5 minuti.

```
git switch main && git pull --ff-only && git merge develop && git push && git switch develop
```

(oppure merge di una PR su `main` direttamente su GitHub: è lo stesso.)

## Comandi

Valgono su tutte e due le macchine: per la produzione vanno lanciati sul server (`ssh kutch@159.173.145.129`).

| cosa | comando |
|---|---|
| deploy subito (senza aspettare il timer) | `systemctl --user start ieg-deploy` |
| ripubblicare anche se il ramo non è cambiato | `~/.local/bin/ieg-deploy.sh --force` (sul PC: `IEG_DEPLOY_BRANCH=develop ~/.local/bin/ieg-deploy.sh --force`) |
| log dei deploy | `journalctl --user -u ieg-deploy` (`-f` per seguirlo) |
| commit pubblicati | `cat ~/.local/share/ieg/deployed/*` |
| prossimo controllo del timer | `systemctl --user list-timers ieg-deploy.timer` |
| fermare / riattivare il deploy automatico | vedi [sotto](#fermare-il-deploy-automatico) |
| stato dei servizi | `systemctl --user status ieg-be caddy cloudflare-tunnel` |
| log del backend | `podman logs -f ieg-be` (anche `~/dev/ieg-be/volume/log/ieg.log`) |
| riavviare il backend | `systemctl --user restart ieg-be` |

### Fermare il deploy automatico

Il timer è quello che controlla il ramo ogni 5 minuti: fermandolo l'ambiente resta com'è (backend e sito continuano
a girare), semplicemente i push non vengono più pubblicati finché non lo si riattiva.

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

# riattivarlo (se nel frattempo il ramo è cambiato, lo pubblica entro 5 minuti)
systemctl --user enable --now ieg-deploy.timer
```

Con il timer fermo si può comunque pubblicare a mano: `systemctl --user start ieg-deploy` (un solo giro).

Interrompere un deploy a metà build del backend lascia in funzione il container di prima; a metà build del frontend
il sito resta com'era (la copia in `www/` si fa solo a build finita). Se lo si interrompe subito dopo il restart di
`ieg-be`, il backend è già quello nuovo ma il commit non risulta pubblicato: al giro successivo viene rifatto.

Per spegnere proprio un ambiente (backend fermo, il sito risponde ma le chiamate alle API falliscono):
`systemctl --user disable --now ieg-deploy.timer && systemctl --user stop ieg-be`. Il quadlet `ieg-be` riparte
comunque al boot: per tenerlo spento anche dopo un riavvio va tolto o commentato `WantedBy=default.target` in
`~/.config/containers/systemd/ieg-be.container`, poi `systemctl --user daemon-reload`.

**Tornare a una versione precedente:** il deploy pubblica sempre la punta del ramo, quindi si fa
`git revert` del commit sbagliato su `main` e push. Il timer pubblica la versione corretta al giro successivo.

## Come funziona il deploy

`ieg-deploy.timer` lancia `ieg-deploy.service` (cioè `~/.local/bin/ieg-deploy.sh`) 2 minuti dopo il boot e poi
ogni 5 minuti. Lo script, per `ieg-be` e `ieg-fe`:

1. fa il fetch del ramo (`main`, o quello di `IEG_DEPLOY_BRANCH`) in un clone dedicato in
   `~/.local/share/ieg/src/<repo>`: **mai da `~/dev`**, quindi quello che si fa in sviluppo (rami, build, immagini
   locali) non tocca l'ambiente;
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

Sul PC il ramo `develop` viene da un drop-in, non dal repository:
`~/.config/systemd/user/ieg-deploy.service.d/test.conf` con `Environment=IEG_DEPLOY_BRANCH=develop`.

## Backup

Solo in produzione. Ogni notte (verso le 4 italiane: il server è in UTC) `ieg-backup.timer` lancia
`deploy/backup.sh`, che salva in `~/backups/ieg` sul server, cifrati con gpg AES256:

- `ieg-db-<data>.dump.gpg`: dump di `ieg-2.1` (`pg_dump -Fc` nel container `postgres`);
- `ieg-files-<data>.tar.gz.gpg`: la cartella `volume/` senza `log/` (`env.properties`, `data/`, `fatture/`).

Si tengono gli ultimi 30 di ciascun tipo. La passphrase è `~/.config/ieg/backup.pass` sul server, con una copia sul PC
in `~/.config/ieg/backup-prod.pass`: **senza, i backup non si aprono**; tenerne una copia anche in un password manager.
Il ripristino è descritto in fondo a `deploy/backup.sh`.

**Copia fuori dal server:** sul PC `prod-backup-pull.timer` (ogni giorno alle 9, o appena il PC si accende) scarica
`~/backups` del server (IEG e daMauro) in `~/backups/prod-ovh`. Usa la chiave `~/.ssh/prod-backup`, che sul server
può solo leggere `~/backups` (`rrsync -ro` in `authorized_keys`). Fallisce, e lo scrive nel log, se l'ultimo backup
ha più di 2 giorni: `journalctl --user -u prod-backup-pull`.

## File e cartelle

Uguali sulle due macchine, salvo dove indicato.

| percorso | cosa |
|---|---|
| `~/dev/ieg-be/deploy/` | sorgenti di script, unit systemd e quadlet (questa cartella; sul server è un clone di `main`) |
| `~/.local/bin/ieg-deploy.sh` | script di deploy installato |
| `~/.config/systemd/user/ieg-deploy.{service,timer}` | unit del deploy automatico |
| `~/.config/systemd/user/ieg-backup.{service,timer}` | unit del backup (solo server) |
| `~/.config/containers/systemd/ieg-be.container` | quadlet del backend (rete host, porta 8080) |
| `~/dev/ieg-be/volume/` | dati dell'ambiente: `env.properties`, `data/`, `fatture/`, `log/` (cartella di lavoro del container) |
| `~/.config/ieg/frontend.env` | variabili di build del frontend (come `.env.local` di ieg-fe, permessi 600) |
| `~/.config/ieg/postgres.env` | password di Postgres (solo server; la stessa è `PG_PASSWORD` in `env.properties`) |
| `~/.config/ieg/backup.pass` | passphrase dei backup (solo server; copia sul PC in `backup-prod.pass`) |
| `~/backups/ieg/` | backup cifrati (solo server; sul PC la copia è in `~/backups/prod-ovh/ieg/`) |
| `~/.local/share/ieg/src/` | cloni di deploy (non modificarli a mano) |
| `~/.local/share/ieg/www/` | frontend pubblicato, servito da Caddy |
| `~/.config/caddy/Caddyfile` | blocco `http://ieg.emmek.org:8076` (server) o `http://ieg-test.emmek.org:8076` (PC) |

Dopo una modifica a qualcosa in `deploy/` (script, unit, quadlet) va rilanciato `deploy/install.sh` su ciascuna
macchina (sul server dopo `git -C ~/dev/ieg-be pull`): copia tutto al suo posto e fa `daemon-reload`. Lo script
installato non si aggiorna da solo col deploy. Per il backup: `deploy/install-backup.sh`.
Dopo una modifica al Caddyfile: `systemctl --user restart caddy` (con `admin off` il reload non funziona).

## Sviluppo, test e produzione

Sul PC test e sviluppo girano insieme senza pestarsi i piedi:

| | test (PC) | dev (PC) |
|---|---|---|
| backend | container `ieg-be`, porta 8080 | `./mvnw spring-boot:run`, porta 8081 |
| cartella di lavoro | `volume/` | `volume-dev/` |
| database | `ieg-2.1` | `ieg-dev` |
| frontend | `https://ieg-test.emmek.org` | `pnpm dev` in ieg-fe (proxy Vite → 8081) |

Porta e database vengono da `PORT_SERVER` e `DB_NAME` nell'`env.properties` di ciascuna cartella.
Avviando il backend da IntelliJ, la working directory va impostata su `volume-dev`.

- **Ricopiare `ieg-dev` dal db di test:**
  ```
  podman exec postgres sh -c 'dropdb -U postgres ieg-dev && createdb -U postgres ieg-dev && pg_dump -U postgres -Fc ieg-2.1 | pg_restore -U postgres -d ieg-dev --no-owner'
  ```
  (fermare prima il backend dev, altrimenti `dropdb` fallisce per le connessioni aperte.)
- **Riportare in test i dati di produzione:** dall'ultimo backup scaricato sul PC, a backend di test fermo
  (`systemctl --user stop ieg-be`):
  ```
  f=$(ls -1t ~/backups/prod-ovh/ieg/ieg-db-*.dump.gpg | head -1)
  podman exec postgres sh -c 'dropdb -U postgres ieg-2.1 && createdb -U postgres ieg-2.1'
  gpg --batch --passphrase-file ~/.config/ieg/backup-prod.pass -d "$f" | podman exec -i postgres pg_restore -U postgres -d ieg-2.1 --no-owner
  ```
- **Modifiche allo schema** (`src/main/resources/schema.sql`): vanno applicate a tutti i database: `ieg-dev` e
  `ieg-2.1` sul PC **prima** del merge su `develop`, `ieg-2.1` sul server **prima** del merge su `main`, altrimenti il
  nuovo backend parte e fallisce con "column … does not exist". Sul server:
  `ssh kutch@159.173.145.129 "podman exec -i postgres psql -U postgres -d ieg-2.1" < modifica.sql`.

## Installazione da zero (nuovo server di produzione)

Riferimento: com'è stato installato il VPS OVH (Fedora 44, utente `kutch` nel gruppo `wheel`).

1. Pacchetti e sistema: `sudo dnf install git podman rsync firewalld`; firewall con solo SSH aperto
   (`sudo systemctl enable --now firewalld`; i servizi ascoltano su localhost o restano dietro il firewall);
   `sudo loginctl enable-linger kutch` (i servizi partono al boot senza login).
2. Quadlet in `~/.config/containers/systemd/`: `postgres.container` (Postgres 18, `PublishPort=127.0.0.1:5432:5432`,
   password da `EnvironmentFile=%h/.config/ieg/postgres.env`), `caddy.container` (rete host, monta
   `%h/.local/share/ieg/www` su `/srv/ieg` e `%h/.config/caddy` su `/etc/caddy`), `cloudflare-tunnel.container`
   (token in `~/.config/cloudflare-tunnel/tunnel.env`). Caddyfile con `default_bind 127.0.0.1` e il blocco
   `http://ieg.emmek.org:8076`.
3. Clonare `ieg-be` in `~/dev/ieg-be` e creare `volume/` con `env.properties` (chiavi elencate in `CLAUDE.md`,
   `PORT_SERVER=8080`, `PG_PASSWORD` uguale a `postgres.env`), `data/`, `fatture/`, `log/`.
4. Creare `~/.config/ieg/frontend.env` (permessi 600) con `VITE_REACT_APP_API_URL=/api`, `VITE_REACT_APP_URL=` e
   `VITE_REACT_APP_PERSIST_KEY=<stringa casuale>`.
5. Dati: ripristinare `ieg-2.1` e `volume/` dall'ultimo backup (vedi [Backup](#backup)), oppure `createdb` e
   `schema.sql` per partire vuoti.
6. `~/dev/ieg-be/deploy/install.sh` e `~/dev/ieg-be/deploy/install-backup.sh` (crea la passphrase: copiarla fuori).
7. Cloudflare: dashboard → **Networks → Tunnels** → tunnel del server → *Public hostname* `ieg.emmek.org` → tipo
   **HTTP** → URL `localhost:8076`. Se c'era un record DNS `ieg` per un altro tunnel va tolto prima, altrimenti il
   CNAME verso il nuovo tunnel non viene creato.
8. `systemctl --user enable --now ieg-deploy.timer` e `systemctl --user start ieg-deploy` per il primo deploy.
