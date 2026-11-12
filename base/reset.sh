#!/usr/bin/env bash
# Remet la base à zéro : supprime la base, la recrée, puis charge init.sql, data.sql et demo.sql.
# ATTENTION : toutes les données de la base sont perdues.
#
#   base/reset.sh              schéma + comptes + données de démo
#   base/reset.sh --sans-demo  schéma + comptes seulement
#   base/reset.sh -y           sans demander de confirmation
#
# Connexion : PGHOST, PGPORT, PGUSER, PGPASSWORD, PGDATABASE
# (par défaut celles de backend/src/main/resources/application.properties).
# Relancer le backend ensuite (il garde des connexions vers l'ancienne base).
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PGHOST="${PGHOST:-localhost}" PGPORT="${PGPORT:-5432}"
export PGUSER="${PGUSER:-postgres}" PGPASSWORD="${PGPASSWORD:-postgres}"
BASE="${PGDATABASE:-gestion_hr}"

demo=1
confirmer=1
for arg in "$@"; do
  case "$arg" in
    --sans-demo) demo=0 ;;
    -y|--oui) confirmer=0 ;;
    -h|--help) sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Option inconnue : $arg (voir --help)" >&2; exit 1 ;;
  esac
done

if ! pg_isready -q; then
  echo "PostgreSQL ne répond pas sur $PGHOST:$PGPORT." >&2
  exit 1
fi

if [ "$confirmer" -eq 1 ]; then
  read -r -p "Supprimer et recréer la base « $BASE » sur $PGHOST:$PGPORT ? Toutes les données seront perdues [o/N] " rep
  [[ "$rep" =~ ^[oOyY]$ ]] || { echo "Annulé."; exit 0; }
fi

echo "→ Suppression de $BASE"
# --force : ferme les connexions ouvertes (backend lancé, client SQL…) — PostgreSQL 13 ou plus
dropdb --if-exists --force "$BASE"
echo "→ Création de $BASE"
createdb "$BASE"

fichiers=(init.sql data.sql)
[ "$demo" -eq 1 ] && fichiers+=(demo.sql)
for f in "${fichiers[@]}"; do
  echo "→ $f"
  psql -q -d "$BASE" -v ON_ERROR_STOP=1 -f "$DIR/$f" >/dev/null
done

echo "Base « $BASE » prête : $(psql -tA -d "$BASE" -c 'SELECT count(*) FROM utilisateur') comptes, $(psql -tA -d "$BASE" -c 'SELECT count(*) FROM demande') demandes."
echo "Relancer le backend s'il tournait."
