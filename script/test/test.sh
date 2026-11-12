#!/usr/bin/env bash
# Lance tous les tests d'API : chaque script *.sh des dossiers de fonctionnalité (auth/, demande/, ...).
# Le backend doit tourner. Usage :
#   script/test/test.sh                         tous les tests, sur http://localhost:8080
#   script/test/test.sh demande                 une seule fonctionnalité
#   API=http://localhost:8081 script/test/test.sh
set -uo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$DIR/lib.sh"
backend_disponible

if [ $# -gt 0 ]; then
  dossiers=("$@")
else
  dossiers=()
  for d in "$DIR"/*/; do dossiers+=("$(basename "$d")"); done
fi

total=0
rates=()
for f in "${dossiers[@]}"; do
  if [ ! -d "$DIR/$f" ]; then
    echo "${ROUGE}Fonctionnalité inconnue : $f${FIN}" >&2
    rates+=("$f"); continue
  fi
  for script in "$DIR/$f"/*.sh; do
    [ -e "$script" ] || continue
    total=$((total + 1))
    printf '\n%s##### %s/%s #####%s\n' "$GRAS" "$f" "$(basename "$script")" "$FIN"
    bash "$script" || rates+=("$f/$(basename "$script")")
  done
done

printf '\n%s==============================%s\n' "$GRAS" "$FIN"
if [ ${#rates[@]} -eq 0 ]; then
  printf '%sTous les scripts passent (%d).%s\n' "$VERT" "$total" "$FIN"
else
  printf '%s%d script(s) en échec sur %d :%s\n' "$ROUGE" "${#rates[@]}" "$total" "$FIN"
  printf '  - %s\n' "${rates[@]}"
  exit 1
fi
