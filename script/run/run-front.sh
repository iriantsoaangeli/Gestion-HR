#!/usr/bin/env bash
# Lance le frontend (Vue + Vite) en mode développement.
set -euo pipefail

FRONT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../frontend" && pwd)"
cd "$FRONT_DIR"

# Installe les dépendances au premier lancement
if [ ! -d node_modules ]; then
  npm install
fi

npm run dev -- "$@"
