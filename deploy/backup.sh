#!/usr/bin/env bash
# Backup cifrato di IEG (server di produzione):
#   - database "ieg-2.1" (pg_dump -Fc nel container "postgres")       -> ieg-db-<data>.dump.gpg
#   - cartella di lavoro volume/ (env.properties, data/, fatture/; senza log/) -> ieg-files-<data>.tar.gz.gpg
# Cifratura gpg AES256 con passphrase in ~/.config/ieg/backup.pass (tenerne una copia FUORI dal server).
# Salvati in ~/backups/ieg, si tengono gli ultimi KEEP (default 30) di ciascun tipo.
# Se BACKUP_COPY_TO è impostata (in ~/.config/ieg/backup.env) ci copia anche i file.
# Avviato ogni notte da systemd (deploy/ieg-backup.timer). Ripristino: vedi in fondo.
set -euo pipefail

CONFIG_DIR="$HOME/.config/ieg"
# shellcheck disable=SC1091
[ -f "$CONFIG_DIR/backup.env" ] && source "$CONFIG_DIR/backup.env" # BACKUP_COPY_TO (facoltativa)
PASSFILE="$CONFIG_DIR/backup.pass"
DB="${DB_NAME:-ieg-2.1}"
VOLUME="${IEG_VOLUME:-$HOME/dev/ieg-be/volume}"
DEST="${BACKUP_DIR:-$HOME/backups/ieg}"
KEEP="${KEEP:-30}"

mkdir -p "$DEST"
chmod 700 "$DEST"
ts=$(date +%Y%m%d-%H%M%S)
db_file="$DEST/ieg-db-$ts.dump.gpg"
files_file="$DEST/ieg-files-$ts.tar.gz.gpg"
cifra() { gpg --batch --yes --quiet --symmetric --cipher-algo AES256 --passphrase-file "$PASSFILE" -o "$1"; }

podman exec postgres pg_dump -U postgres -Fc "$DB" | cifra "$db_file"
tar -C "$VOLUME" --exclude=./log -czf - . | cifra "$files_file"
chmod 600 "$db_file" "$files_file"

# un file vuoto vuol dire che il dump non è riuscito
for f in "$db_file" "$files_file"; do
  if [ ! -s "$f" ]; then
    echo "backup vuoto: $f" >&2
    rm -f "$db_file" "$files_file"
    exit 1
  fi
done

pulisci() { ls -1t "$1"/ieg-db-*.dump.gpg 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -f
            ls -1t "$1"/ieg-files-*.tar.gz.gpg 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -f; }

if [ -n "${BACKUP_COPY_TO:-}" ]; then
  mkdir -p "$BACKUP_COPY_TO"
  cp "$db_file" "$files_file" "$BACKUP_COPY_TO/"
  pulisci "$BACKUP_COPY_TO"
fi
pulisci "$DEST"
echo "backup salvato: $db_file ($(du -h "$db_file" | cut -f1)), $files_file ($(du -h "$files_file" | cut -f1))"

# Ripristino del database (in un db nuovo, poi si scambia; oppure dropdb/createdb di ieg-2.1 a backend fermo):
#   gpg --batch --passphrase-file ~/.config/ieg/backup.pass -d ieg-db-XXXX.dump.gpg |
#     podman exec -i postgres sh -c 'createdb -U postgres ieg-ripristino && pg_restore -U postgres -d ieg-ripristino --no-owner'
# Ripristino dei file (in una cartella a parte, poi si copia quello che serve in volume/):
#   mkdir /tmp/ieg-files && gpg --batch --passphrase-file ~/.config/ieg/backup.pass -d ieg-files-XXXX.tar.gz.gpg | tar -C /tmp/ieg-files -xzf -
