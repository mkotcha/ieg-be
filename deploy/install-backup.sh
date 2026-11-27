#!/usr/bin/env bash
# Installa il timer systemd (utente) del backup notturno di IEG: deploy/ieg-backup.{service,timer}.
# Solo sul server di produzione. Al primo giro crea la passphrase ~/.config/ieg/backup.pass:
# va copiata FUORI dal server (senza, i backup non si possono decifrare).
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f "$HOME/.config/ieg/backup.pass" ]; then
  (umask 077; mkdir -p "$HOME/.config/ieg"; openssl rand -base64 48 > "$HOME/.config/ieg/backup.pass")
  echo "creata ~/.config/ieg/backup.pass: COPIALA FUORI DAL SERVER"
fi
install -Dm 644 ieg-backup.service "$HOME/.config/systemd/user/ieg-backup.service"
install -Dm 644 ieg-backup.timer "$HOME/.config/systemd/user/ieg-backup.timer"
systemctl --user daemon-reload
systemctl --user enable --now ieg-backup.timer
systemctl --user list-timers ieg-backup.timer --no-pager
