#!/usr/bin/env bash
# Installa (o aggiorna) il deploy automatico di IEG per l'utente corrente:
#   - ~/.local/bin/ieg-deploy.sh
#   - ~/.config/systemd/user/ieg-deploy.{service,timer}
#   - ~/.config/containers/systemd/ieg-be.container (quadlet del backend)
# Il timer NON viene attivato da qui: va acceso a mano quando main su GitHub (ieg-be e ieg-fe) è pronto:
#   systemctl --user enable --now ieg-deploy.timer
# Prerequisito: ~/.config/ieg/frontend.env con le VITE_REACT_APP_* (come .env.local di ieg-fe).
set -euo pipefail
cd "$(dirname "$0")"

install -Dm 755 ieg-deploy.sh "$HOME/.local/bin/ieg-deploy.sh"
install -Dm 644 ieg-deploy.service "$HOME/.config/systemd/user/ieg-deploy.service"
install -Dm 644 ieg-deploy.timer "$HOME/.config/systemd/user/ieg-deploy.timer"
install -Dm 644 ieg-be.container "$HOME/.config/containers/systemd/ieg-be.container"
systemctl --user daemon-reload

[[ -f "$HOME/.config/ieg/frontend.env" ]] || echo "ATTENZIONE: manca ~/.config/ieg/frontend.env (serve alla build del frontend)"
echo "installato. Timer: systemctl --user enable --now ieg-deploy.timer  -  log: journalctl --user -u ieg-deploy"
