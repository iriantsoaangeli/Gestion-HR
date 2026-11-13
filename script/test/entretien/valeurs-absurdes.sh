#!/usr/bin/env bash
# Entretiens et évaluations : valeurs absurdes sur /api/entretiens et /api/evaluations. Contrat : doc/routes/api/entretiens.md.
# S'appuie sur la démo du recrutement (base/demo.sql) : un dossier de l'offre ouverte « Comptable », un dossier de l'offre close.
# Le script prépare un entretien (hr) avec deux interviewers (hr et manager), vérifie qu'on ne l'évalue pas avant sa date,
# le date d'il y a une heure (psql), puis teste l'évaluation et les notes séparées.
# Les ids sont lus avec psql, et les entretiens créés (interviewers, évaluations) sont supprimés à la fin
# (PGHOST, PGUSER, PGPASSWORD, PGDATABASE ; valeurs par défaut = backend/src/main/resources/application.properties).
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

E=/api/entretiens
V=/api/evaluations
ENTRETIENS=()

sql() {
  PGPASSWORD="${PGPASSWORD:-postgres}" psql -tA -h "${PGHOST:-localhost}" -U "${PGUSER:-postgres}" \
    -d "${PGDATABASE:-gestion_hr}" -v ON_ERROR_STOP=1 -c "$1"
}
champ() { grep -o "\"$1\":[0-9]*" "$TMP_TESTS/corps" | head -1 | grep -o '[0-9]*$'; }
poste_de() { sql "SELECT idPoste FROM utilisateur WHERE login = '$1'"; }

DOSSIER=$(sql "SELECT d.id FROM dossier d JOIN offre o ON o.id = d.idOffre WHERE o.status = 'OUVERT' ORDER BY d.id LIMIT 1")
DOSSIER_CLOS=$(sql "SELECT d.id FROM dossier d JOIN offre o ON o.id = d.idOffre WHERE o.status = 'CLOS' ORDER BY d.id LIMIT 1")
P_HR=$(poste_de hr); P_MANAGER=$(poste_de manager); P_FINANCE=$(poste_de finance); P_ADMIN=$(poste_de admin)
if [ -z "$DOSSIER" ] || [ -z "$DOSSIER_CLOS" ] || [ -z "$P_HR" ] || [ -z "$P_MANAGER" ]; then
  echo "${ROUGE}Démo du recrutement absente (base/demo.sql) ou psql injoignable.${FIN}" >&2
  exit 2
fi

nettoyer() {
  [ ${#ENTRETIENS[@]} -eq 0 ] && return
  local ids; ids=$(IFS=,; echo "${ENTRETIENS[*]}")
  if sql "BEGIN;
DELETE FROM evaluation  WHERE idEntretien IN ($ids);
DELETE FROM interviewer WHERE idEntretien IN ($ids);
DELETE FROM entretien   WHERE id IN ($ids);
COMMIT;" >/dev/null 2>&1; then
    printf '\n%sEntretiens de test supprimés : %s%s\n' "$GRIS" "$ids" "$FIN"
  else
    printf '\n%sImpossible de supprimer les entretiens de test (psql) : %s%s\n' "$ROUGE" "$ids" "$FIN"
  fi
}
trap 'nettoyer; rm -rf "$TMP_TESTS"' EXIT

FUTUR="\"$(date -d '+10 days' +%F)T10:00:00\""
# corps DOSSIER DATE INTERVIEWERS : JSON brut
corps() { printf '{"idDossier":%s,"date":%s,"interviewers":%s}' "$1" "$2" "$3"; }
valide() { corps "$DOSSIER" "$FUTUR" "[$P_HR,$P_MANAGER]"; }
note() { printf '{"note":%s,"remarques":%s}' "$1" "$2"; }

# Vérifie qu'un texte est dans la dernière réponse
contient() {
  if grep -q "$2" "$TMP_TESTS/corps"; then
    REUSSIS=$((REUSSIS + 1)); echo "  ${VERT}OK${FIN}    $1"
  else
    ECHECS=$((ECHECS + 1)); echo "  ${ROUGE}ECHEC${FIN} $1 : $(head -c 300 "$TMP_TESTS/corps")"
  fi
}

# ---------------------------------------------------------------------------
section "Entretien — droits"
session_anonyme
verifier "en attente sans être connecté"     401:NON_CONNECTE GET "$E/en-attente"
verifier "préparer sans être connecté"       401:NON_CONNECTE POST $E "$(valide)"
connecter finance finance123
verifier "en attente en tant que Finance"    403:ACCES_REFUSE GET "$E/en-attente"
verifier "mes évaluations (Finance)"         403:ACCES_REFUSE GET "$V/a-faire"
connecter admin admin123
verifier "en attente en tant qu'admin"       403:ACCES_REFUSE GET "$E/en-attente"
connecter manager manager123
verifier "en attente en tant que manager"    200 GET "$E/en-attente"
verifier "préparer en tant que manager"      403:ACCES_REFUSE POST $E "$(valide)"
verifier "interviewers possibles (manager)"  403:ACCES_REFUSE GET "$E/interviewers-possibles"
connecter hr hr123
JETON= verifier "préparer sans jeton CSRF"   403 POST $E "$(valide)"

section "Entretien — corps de la requête"
verifier "JSON cassé"                        400:REQUETE_ILLISIBLE POST $E '{"idDossier":1,'
verifier "tableau au lieu d'objet"           400:REQUETE_ILLISIBLE POST $E '[]'
verifier "objet vide {}"                     400:VALIDATION        POST $E '{}'
verifier "texte brut (pas JSON)"             415                   POST $E '' -H 'Content-Type: text/plain' --data 'bonjour'

section "Entretien — dossier"
for v in null -1 0 999999 2147483647; do
  verifier "idDossier = $v"                  400:VALIDATION POST $E "$(corps "$v" "$FUTUR" "[$P_HR]")"
done
verifier "idDossier = \"abc\""               400:REQUETE_ILLISIBLE POST $E "$(corps '"abc"' "$FUTUR" "[$P_HR]")"
verifier "idDossier = 99999999999 (> int)"   400:REQUETE_ILLISIBLE POST $E "$(corps 99999999999 "$FUTUR" "[$P_HR]")"
verifier "dossier d'une offre close"         400:VALIDATION POST $E "$(corps "$DOSSIER_CLOS" "$FUTUR" "[$P_HR]")"

section "Entretien — date (dans le futur)"
verifier "date = null"                       400:VALIDATION POST $E "$(corps "$DOSSIER" null "[$P_HR]")"
for v in "$(date -d '-1 hour' +%FT%T)" "$(date -d '-1 day' +%FT%T)" 2000-01-01T10:00:00; do
  verifier "date = $v (passée)"              400:VALIDATION POST $E "$(corps "$DOSSIER" "\"$v\"" "[$P_HR]")"
done
for v in 2027-02-30T10:00:00 2027-01-01T25:00:00 2027-01-01 '18/11/2027 10:00' demain ''; do
  verifier "date = \"$v\" (invalide)"        400 POST $E "$(corps "$DOSSIER" "\"$v\"" "[$P_HR]")"
done
verifier "date = 12"                         400 POST $E "$(corps "$DOSSIER" 12 "[$P_HR]")"

section "Entretien — interviewers"
verifier "interviewers = null"               400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" null)"
verifier "interviewers = []"                 400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" '[]')"
verifier "interviewers = [null]"             400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" '[null]')"
for v in 0 -1 999999; do
  verifier "interviewer = $v (inconnu)"      400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" "[$v]")"
done
verifier "poste du compte Finance"           400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" "[$P_FINANCE]")"
verifier "poste du compte admin"             400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" "[$P_ADMIN]")"
verifier "même interviewer deux fois"        400:VALIDATION POST $E "$(corps "$DOSSIER" "$FUTUR" "[$P_HR,$P_HR]")"
verifier "interviewers = \"abc\""            400:REQUETE_ILLISIBLE POST $E "$(corps "$DOSSIER" "$FUTUR" '"abc"')"
verifier "interviewers = [\"abc\"]"          400:REQUETE_ILLISIBLE POST $E "$(corps "$DOSSIER" "$FUTUR" '["abc"]')"

section "Entretien — préparation (contrôle)"
verifier "préparer avec hr et manager (contrôle)" 201 POST $E "$(valide)"
ENT="$(champ id)"; [ -n "$ENT" ] && ENTRETIENS+=("$ENT")
contient "statut EN_ATTENTE" '"statut":"EN_ATTENTE"'
verifier "un 2e entretien pour le même dossier" 201 POST $E "$(valide)"
e2="$(champ id)"; [ -n "$e2" ] && ENTRETIENS+=("$e2")

section "Évaluation — droits"
connecter direction direction123
verifier "évaluer sans être interviewer"     403:ACCES_REFUSE POST "$E/$ENT/evaluation" "$(note 50 '"Remarques"')"
verifier "voir l'entretien (Direction)"      200 GET "$E/$ENT"
connecter hr hr123
JETON= verifier "évaluer sans jeton CSRF"    403 POST "$E/$ENT/evaluation" "$(note 50 '"Remarques"')"
verifier "évaluer l'entretien 999999"        404:RESSOURCE_INTROUVABLE POST "$E/999999/evaluation" "$(note 50 '"Remarques"')"

section "Évaluation — date"
verifier "évaluer avant la date"             409:ENTRETIEN_A_VENIR POST "$E/$ENT/evaluation" "$(note 50 '"Remarques"')"
# La préparation n'accepte qu'une date future : on fait comme si l'entretien avait eu lieu
sql "UPDATE entretien SET date = now() - interval '1 hour' WHERE id = $ENT" >/dev/null

section "Évaluation — note et remarques"
for v in null -1 101 1000 50.5 0.1 -0.5; do
  verifier "note = $v"                       400:VALIDATION POST "$E/$ENT/evaluation" "$(note "$v" '"Remarques"')"
done
for v in '"abc"' '"50"' true '[50]'; do
  verifier "note = $v"                       400 POST "$E/$ENT/evaluation" "$(note "$v" '"Remarques"')"
done
for v in null '""' '"   "' '"\n\t"'; do
  verifier "remarques = $v"                  400:VALIDATION POST "$E/$ENT/evaluation" "$(note 50 "$v")"
done
verifier "remarques de 5001 caractères"      400:VALIDATION POST "$E/$ENT/evaluation" "$(note 50 "\"$(printf 'r%.0s' {1..5001})\"")"
verifier "remarques objet"                   400:REQUETE_ILLISIBLE POST "$E/$ENT/evaluation" "$(note 50 '{"a":1}')"

section "Évaluation — notes séparées"
verifier "hr évalue (contrôle, note 0)"      201 POST "$E/$ENT/evaluation" "$(note 0 '"Remarques de hr"')"
EVAL_HR="$(champ idEvaluation)"
contient "toujours EN_ATTENTE (manager n'a pas évalué)" '"statut":"EN_ATTENTE"'
verifier "hr évalue une 2e fois"             409:DEJA_FAIT POST "$E/$ENT/evaluation" "$(note 60 '"Encore"')"
connecter manager manager123
verifier "manager voit l'entretien"          200 GET "$E/$ENT"
contient "notes cachées au manager" '"notesCachees":true'
[ -n "$EVAL_HR" ] && verifier "fiche de l'évaluation de hr (cachée)" 403:ACCES_REFUSE GET "$V/$EVAL_HR"
verifier "manager évalue (contrôle, note 100)" 201 POST "$E/$ENT/evaluation" "$(note 100 '"Remarques du manager"')"
contient "statut FINI (tous ont évalué)" '"statut":"FINI"'
contient "note moyenne 50" '"note":50'
[ -n "$EVAL_HR" ] && verifier "fiche de l'évaluation de hr (visible)" 200 GET "$V/$EVAL_HR"

section "Entretien — lecture"
verifier "finis"                             200 GET "$E/finis"
verifier "tous"                              200 GET $E
verifier "entretiens d'une offre inconnue"   404:RESSOURCE_INTROUVABLE GET "$E?offre=999999"
verifier "entretiens de l'offre « abc »"     4xx GET "$E?offre=abc"
verifier "entretien 0"                       404:RESSOURCE_INTROUVABLE GET "$E/0"
verifier "entretien « abc »"                 4xx GET "$E/abc"
verifier "entretien 99999999999"             4xx GET "$E/99999999999"
verifier "mes évaluations à faire"           200 GET "$V/a-faire"
verifier "mes évaluations faites"            200 GET "$V/faites"
verifier "évaluation 0"                      404:RESSOURCE_INTROUVABLE GET "$V/0"
verifier "évaluation « abc »"                4xx GET "$V/abc"

bilan
