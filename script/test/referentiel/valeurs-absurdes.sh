#!/usr/bin/env bash
# Référentiel : valeurs absurdes sur /api/referentiel (lecture seule). Ne modifie pas la base.
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

POSTES=/api/referentiel/postes

section "Référentiel — sans connexion"
session_anonyme
verifier "départements sans être connecté"     401:NON_CONNECTE GET /api/referentiel/departements
verifier "fonctions sans être connecté"        401:NON_CONNECTE GET /api/referentiel/fonctions
verifier "postes sans être connecté"           401:NON_CONNECTE GET "$POSTES?departement=1"

section "Référentiel — paramètre departement"
connecter manager manager123
verifier "departement=1 (contrôle)"            200 GET "$POSTES?departement=1"
verifier "paramètre absent"                    400 GET "$POSTES"
verifier "departement vide"                    400 GET "$POSTES?departement="
verifier "departement=abc"                     400 GET "$POSTES?departement=abc"
verifier "departement=1.5"                     400 GET "$POSTES?departement=1.5"
verifier "departement=1e3"                     400 GET "$POSTES?departement=1e3"
verifier "departement=99999999999 (> int)"     400 GET "$POSTES?departement=99999999999"
verifier "departement=-1 (liste vide)"         200 GET "$POSTES?departement=-1"
verifier "departement=0 (liste vide)"          200 GET "$POSTES?departement=0"
verifier "departement inexistant (liste vide)" 200 GET "$POSTES?departement=2147483647"
verifier "injection SQL"                       400 GET "$POSTES?departement=1%20OR%201%3D1"

section "Référentiel — postes des comptes"
verifier "mon département (manager)"           200 GET /api/referentiel/mon-departement
grep -q '"label":"Informatique"' "$TMP_TESTS/corps" || { ECHECS=$((ECHECS + 1)); echo "  ${ROUGE}ECHEC${FIN} mon département de manager ≠ Informatique"; }
verifier "postes du département 2"             200 GET "$POSTES?departement=2"
if grep -q '"id":6,' "$TMP_TESTS/corps" && ! grep -q '"id":10,' "$TMP_TESTS/corps"; then
  REUSSIS=$((REUSSIS + 1)); echo "  ${VERT}OK${FIN}    son propre poste (6) proposé, pas celui d'admin (10)"
else
  ECHECS=$((ECHECS + 1)); echo "  ${ROUGE}ECHEC${FIN} postes remplaçables : $(cat "$TMP_TESTS/corps")"
fi
session_anonyme
verifier "mon département sans être connecté"  401:NON_CONNECTE GET /api/referentiel/mon-departement
connecter manager manager123

section "Référentiel — méthodes non prévues"
verifier "POST sur départements"               4xx POST   /api/referentiel/departements '{"label":"x"}'
verifier "DELETE sur fonctions"                4xx DELETE /api/referentiel/fonctions
verifier "route inconnue"                      404 GET    /api/referentiel/inconnu

bilan
