#!/usr/bin/env bash
# Offres d'emploi : valeurs absurdes sur /api/offres (publication, lecture). Contrat : doc/routes/api/offres.md.
# Le script crée deux demandes (manager) : une approuvée (hr, direction, finance) d'où partent les offres,
# une laissée en attente. Chaque cas part d'un corps valide et ne change qu'un champ.
# Tout ce qui a été créé (demandes, offres, postes vides) est supprimé à la fin avec psql
# (PGHOST, PGUSER, PGPASSWORD, PGDATABASE ; valeurs par défaut = backend/src/main/resources/application.properties).
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

OFFRES=/api/offres
DEMANDES=()

# Id de la ressource renvoyée par le dernier appel ({"id":N,...}), vide sinon.
id_reponse() { grep -o '^{"id":[0-9]*' "$TMP_TESTS/corps" | grep -o '[0-9]*$'; }

# Demande du manager (Informatique) : 2 Développeurs (fonction 3) + remplacement du poste 4
DEMANDE='{"idDepartement":2,"motif":"Test offres (valeurs absurdes)","creations":[{"idFonction":3,"effectif":2}],"remplacements":[4]}'
nouvelle_demande() {
  connecter manager manager123
  verifier "$1" 201 POST /api/demandes "$DEMANDE"
  local id; id="$(id_reponse)"
  [ -n "$id" ] && DEMANDES+=("$id")
  echo "${id:-0}" > "$TMP_TESTS/id"
}

# corps DEMANDE FONCTION POSTE INTITULE DESCRIPTION PROFIL DELAI : chaque argument est du JSON brut
corps() {
  printf '{"idDemande":%s,"idFonction":%s,"idPoste":%s,"intitule":%s,"description":%s,"profil":%s,"delai":%s}' \
    "$1" "$2" "$3" "$4" "$5" "$6" "$7"
}

nettoyer() {
  [ ${#DEMANDES[@]} -eq 0 ] && return
  local ids; ids=$(IFS=,; echo "${DEMANDES[*]}")
  if PGPASSWORD="${PGPASSWORD:-postgres}" psql -q -h "${PGHOST:-localhost}" -U "${PGUSER:-postgres}" \
      -d "${PGDATABASE:-gestion_hr}" -v ON_ERROR_STOP=1 >/dev/null 2>&1 <<SQL
BEGIN;
-- Postes vides créés par les offres de test (les postes remplacés existaient avant : on les garde)
CREATE TEMP TABLE zz_postes ON COMMIT DROP AS
  SELECT op.idPoste AS id FROM offrePoste op JOIN offre o ON o.id = op.idOffre
  WHERE o.idDemande IN ($ids) AND op.idPoste IN (SELECT idPoste FROM creationPoste);
DELETE FROM offrePoste    WHERE idOffre IN (SELECT id FROM offre WHERE idDemande IN ($ids));
DELETE FROM offre         WHERE idDemande IN ($ids);
DELETE FROM fonctionPoste WHERE idPoste IN (SELECT id FROM zz_postes);
DELETE FROM emploi        WHERE idPoste IN (SELECT id FROM zz_postes);
DELETE FROM creationPoste WHERE idPoste IN (SELECT id FROM zz_postes);
DELETE FROM histoPoste    WHERE idPoste IN (SELECT id FROM zz_postes);
DELETE FROM poste         WHERE id IN (SELECT id FROM zz_postes);
DELETE FROM demandeFonction WHERE idDemande IN ($ids);
DELETE FROM demandePoste    WHERE idDemande IN ($ids);
DELETE FROM histoDemande    WHERE idDemande IN ($ids);
DELETE FROM demande         WHERE id IN ($ids);
COMMIT;
SQL
  then
    printf '\n%sDemandes de test supprimées, avec leurs offres et postes : %s%s\n' "$GRIS" "$ids" "$FIN"
  else
    printf '\n%sImpossible de supprimer les données de test (psql), demandes : %s%s\n' "$ROUGE" "$ids" "$FIN"
  fi
}
trap 'nettoyer; rm -rf "$TMP_TESTS"' EXIT

# ---------------------------------------------------------------------------
section "Préparation : une demande approuvée, une demande en attente"
nouvelle_demande "demande à approuver (contrôle)"
APPROUVEE="$(cat "$TMP_TESTS/id")"
for v in "hr hr123" "direction direction123" "finance finance123"; do
  connecter $v
  verifier "validation par ${v%% *}" 200 POST "/api/demandes/$APPROUVEE/valider"
done
nouvelle_demande "demande laissée en attente HR"
EN_ATTENTE="$(cat "$TMP_TESTS/id")"

# Corps valide de référence : création de la fonction 3 de la demande approuvée
DEM=$APPROUVEE; FON=3; POS=null; INT='"Test valeurs absurdes"'; DES='"Description de test"'; PRO=null
DEL="\"$(date_dans 30)\""
valide() { corps "$DEM" "$FON" "$POS" "$INT" "$DES" "$PRO" "$DEL"; }

# ---------------------------------------------------------------------------
section "Offre — droits"
session_anonyme
verifier "lister sans être connecté"         401:NON_CONNECTE GET $OFFRES
verifier "publier sans être connecté"        401:NON_CONNECTE POST $OFFRES "$(valide)"
connecter manager manager123
verifier "lister en tant que manager"        403:ACCES_REFUSE GET $OFFRES
verifier "fiche en tant que manager"         403:ACCES_REFUSE GET "$OFFRES/1"
verifier "demandes approuvées (manager)"     403:ACCES_REFUSE GET "$OFFRES/demandes-approuvees"
verifier "publier en tant que manager"       403:ACCES_REFUSE POST $OFFRES "$(valide)"
connecter finance finance123
verifier "publier en tant que Finance"       403:ACCES_REFUSE POST $OFFRES "$(valide)"
connecter hr hr123
JETON= verifier "publier sans jeton CSRF"    403 POST $OFFRES "$(valide)"
JETON=faux verifier "publier avec un faux jeton CSRF" 403 POST $OFFRES "$(valide)"

section "Offre — corps de la requête"
verifier "JSON cassé"                        400:REQUETE_ILLISIBLE POST $OFFRES '{"idDemande":1,'
verifier "tableau au lieu d'objet"           400:REQUETE_ILLISIBLE POST $OFFRES '[]'
verifier "objet vide {}"                     400:VALIDATION        POST $OFFRES '{}'
verifier "corps vide"                        400                   POST $OFFRES '' -H 'Content-Type: application/json'
verifier "texte brut (pas JSON)"             415                   POST $OFFRES '' -H 'Content-Type: text/plain' --data 'bonjour'

section "Offre — intitulé, description, profil"
for v in null '""' '"   "' '"\n\t"'; do
  verifier "intitulé = $v"                   400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$v" "$DES" "$PRO" "$DEL")"
  verifier "description = $v"                400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$v" "$PRO" "$DEL")"
done
verifier "intitulé de 151 caractères"        400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "\"$(printf 'i%.0s' {1..151})\"" "$DES" "$PRO" "$DEL")"
verifier "description de 4001 caractères"    400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "\"$(printf 'd%.0s' {1..4001})\"" "$PRO" "$DEL")"
verifier "profil de 4001 caractères"         400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$DES" "\"$(printf 'p%.0s' {1..4001})\"" "$DEL")"
verifier "description de 100 000 caractères" 400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "\"$(printf 'd%.0s' {1..100000})\"" "$PRO" "$DEL")"
verifier "intitulé tableau"                  400:REQUETE_ILLISIBLE POST $OFFRES "$(corps "$DEM" "$FON" "$POS" '["a"]' "$DES" "$PRO" "$DEL")"
verifier "description objet"                 400:REQUETE_ILLISIBLE POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" '{"a":1}' "$PRO" "$DEL")"

section "Offre — date limite (au moins demain)"
for v in "$(date_dans 0)" "$(date_dans -1)" "$(date_dans -365)" 0000-01-01 1900-01-01; do
  verifier "date limite = $v"                400:VALIDATION POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$DES" "$PRO" "\"$v\"")"
done
for v in 2027-02-30 2027-13-01 2027-00-10 2027-01-32 2027-1-1 31/12/2027 demain ''; do
  verifier "date limite = \"$v\" (invalide)" 400 POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$DES" "$PRO" "\"$v\"")"
done
verifier "date limite = true"                400:REQUETE_ILLISIBLE POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$DES" "$PRO" true)"
verifier "date limite = date + heure"        400 POST $OFFRES "$(corps "$DEM" "$FON" "$POS" "$INT" "$DES" "$PRO" '"2027-06-01T10:00:00"')"

section "Offre — demande d'origine"
for v in null -1 0 999999 2147483647; do
  verifier "idDemande = $v"                  400:VALIDATION POST $OFFRES "$(corps "$v" "$FON" "$POS" "$INT" "$DES" "$PRO" "$DEL")"
done
verifier "idDemande = 99999999999 (> int)"   400:REQUETE_ILLISIBLE POST $OFFRES "$(corps 99999999999 "$FON" "$POS" "$INT" "$DES" "$PRO" "$DEL")"
verifier "idDemande = \"abc\""               400:REQUETE_ILLISIBLE POST $OFFRES "$(corps '"abc"' "$FON" "$POS" "$INT" "$DES" "$PRO" "$DEL")"
verifier "demande pas encore approuvée"      400:VALIDATION POST $OFFRES "$(corps "$EN_ATTENTE" "$FON" "$POS" "$INT" "$DES" "$PRO" "$DEL")"

section "Offre — ligne de la demande (une création ou un remplacement)"
verifier "ni création ni remplacement"       400:VALIDATION POST $OFFRES "$(corps "$DEM" null null "$INT" "$DES" "$PRO" "$DEL")"
verifier "création et remplacement à la fois" 400:VALIDATION POST $OFFRES "$(corps "$DEM" 3 4 "$INT" "$DES" "$PRO" "$DEL")"
for v in -1 0 1 999999; do
  verifier "idFonction = $v (pas dans la demande)" 400:VALIDATION POST $OFFRES "$(corps "$DEM" "$v" null "$INT" "$DES" "$PRO" "$DEL")"
done
for v in -1 0 3 999999; do
  verifier "idPoste = $v (pas dans la demande)" 400:VALIDATION POST $OFFRES "$(corps "$DEM" null "$v" "$INT" "$DES" "$PRO" "$DEL")"
done
verifier "idFonction = \"abc\""              400:REQUETE_ILLISIBLE POST $OFFRES "$(corps "$DEM" '"abc"' null "$INT" "$DES" "$PRO" "$DEL")"
verifier "idPoste = [4]"                     400:REQUETE_ILLISIBLE POST $OFFRES "$(corps "$DEM" null '[4]' "$INT" "$DES" "$PRO" "$DEL")"

section "Offre — publication (contrôle) et doublons"
verifier "création de 2 postes (contrôle)"   201 POST $OFFRES "$(valide)"
OFFRE="$(id_reponse)"
if grep -q "\"code\":\"INF-$(date +%Y)-[0-9]\{4\}\"" "$TMP_TESTS/corps"; then
  REUSSIS=$((REUSSIS + 1)); echo "  ${VERT}OK${FIN}    postes créés avec un code INF-$(date +%Y)-NNNN"
else
  ECHECS=$((ECHECS + 1)); echo "  ${ROUGE}ECHEC${FIN} code des postes créés : $(head -c 300 "$TMP_TESTS/corps")"
fi
verifier "republier la même création"        409:DEJA_FAIT    POST $OFFRES "$(valide)"
verifier "remplacement du poste 4 (contrôle)" 201 POST $OFFRES "$(corps "$DEM" null 4 "$INT" "$DES" "$PRO" null)"
verifier "republier le même remplacement"    409:DEJA_FAIT    POST $OFFRES "$(corps "$DEM" null 4 "$INT" "$DES" "$PRO" null)"

section "Offre — lecture"
verifier "liste des offres (HR)"             200 GET $OFFRES
verifier "demandes approuvées (HR)"          200 GET "$OFFRES/demandes-approuvees"
[ -n "$OFFRE" ] && verifier "fiche de l'offre publiée"  200 GET "$OFFRES/$OFFRE"
verifier "fiche de l'offre 0"                404:RESSOURCE_INTROUVABLE GET "$OFFRES/0"
verifier "fiche de l'offre 2147483647"       404:RESSOURCE_INTROUVABLE GET "$OFFRES/2147483647"
verifier "fiche de l'offre -1"               4xx GET "$OFFRES/-1"
verifier "fiche de l'offre « abc »"          4xx GET "$OFFRES/abc"
verifier "fiche de l'offre 99999999999"      400 GET "$OFFRES/99999999999"

bilan
