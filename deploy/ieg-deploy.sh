#!/usr/bin/env bash
# Allinea prod al ramo main su GitHub (ieg-be e ieg-fe). Lo lancia ogni 5 minuti ieg-deploy.timer.
#
# Per ogni repository: fetch di origin/main; se il commit è diverso da quello già pubblicato
# (~/.local/share/ieg/deployed/<repo>), si porta il clone di deploy su quel commit e:
#   ieg-be -> podman build dell'immagine localhost/ieg-be:latest e restart del container ieg-be
#   ieg-fe -> pnpm build in un container Node e copia di dist/ in ~/.local/share/ieg/www (servita da Caddy)
# Il commit si segna come pubblicato solo se tutto è andato bene: in caso di errore il servizio
# fallisce (journalctl --user -u ieg-deploy) e al giro successivo si riprova.
#
#   ieg-deploy.sh            pubblica solo i repository cambiati
#   ieg-deploy.sh --force    ripubblica comunque entrambi
# IEG_DEPLOY_DIR, IEG_GIT_BASE e IEG_DEPLOY_BRANCH servono solo per provarlo (cartella, origine, ramo).
set -euo pipefail

BASE="${IEG_DEPLOY_DIR:-$HOME/.local/share/ieg}"
FRONTEND_ENV="${IEG_FRONTEND_ENV:-$HOME/.config/ieg/frontend.env}"
GIT_BASE="${IEG_GIT_BASE:-https://github.com/mkotcha}"
BRANCH="${IEG_DEPLOY_BRANCH:-main}"
NODE_IMAGE="docker.io/library/node:22-slim"
PNPM_VERSION="10.34.9"
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

mkdir -p "$BASE/src" "$BASE/www" "$BASE/deployed"
exec 9>"$BASE/.lock"
flock -n 9 || { echo "deploy già in corso, salto"; exit 0; }

deploy_ieg_be() {
    local dir=$1
    podman build -q -t localhost/ieg-be:latest "$dir"
    systemctl --user restart ieg-be
    # aspetta che Spring sia in ascolto prima di dichiarare il deploy riuscito
    timeout 180 bash -c 'until ss -ltn | grep -q ":8080 "; do sleep 3; done' \
        || { echo "ieg-be non risponde sulla 8080 dopo il restart"; return 1; }
    podman image prune -f >/dev/null
}

deploy_ieg_fe() {
    local dir=$1
    # VITE_REACT_APP_* (chiave di redux-persist compresa) non sono nel repository
    install -m 600 "$FRONTEND_ENV" "$dir/.env.production.local"
    podman run --rm --security-opt label=disable \
        -v "$dir":/app -w /app \
        -v ieg-pnpm-store:/root/.local/share/pnpm/store \
        "$NODE_IMAGE" \
        sh -c "npm install -g --silent pnpm@$PNPM_VERSION && pnpm install --frozen-lockfile && pnpm build"
    # copia a build finita: se la build fallisce il sito resta quello di prima
    rsync -a --delete "$dir/dist/" "$BASE/www/"
}

for repo in ieg-be ieg-fe; do
    dir="$BASE/src/$repo"
    [[ -d "$dir/.git" ]] || git clone -q "$GIT_BASE/$repo" "$dir"
    git -C "$dir" fetch -q origin "$BRANCH"
    nuovo=$(git -C "$dir" rev-parse FETCH_HEAD)
    vecchio=$(cat "$BASE/deployed/$repo" 2>/dev/null || true)
    if [[ "$nuovo" == "$vecchio" && $FORCE -eq 0 ]]; then
        continue
    fi
    echo "$repo: ${vecchio:0:7} -> ${nuovo:0:7} ($(git -C "$dir" log -1 --format=%s "$nuovo"))"
    git -C "$dir" checkout -q --detach "$nuovo"
    git -C "$dir" clean -q -fdx -e node_modules
    "deploy_${repo//-/_}" "$dir"
    echo "$nuovo" > "$BASE/deployed/$repo"
    echo "$repo: pubblicato ${nuovo:0:7}"
done
