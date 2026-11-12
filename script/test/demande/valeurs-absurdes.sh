#!/usr/bin/env bash
# Demandes : valeurs absurdes sur /api/demandes (création, décision, lecture).
# Chaque cas part d'un corps valide et ne change qu'un champ.
# Les demandes créées (contrôles, ou valeurs absurdes acceptées à tort) sont supprimées à la fin avec psql
# (PGHOST, PGUSER, PGPASSWORD, PGDATABASE ; valeurs par défaut = backend/src/main/resources/application.properties).
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

DEMANDES=/api/demandes
CREEES=()

# corps DEPARTEMENT MOTIF DELAI CREATIONS REMPLACEMENTS : chaque argument est du JSON brut
corps() {
  printf '{"idDepartement":%s,"motif":%s,"delai":%s,"creations":%s,"remplacements":%s}' "$1" "$2" "$3" "$4" "$5"
}
# Corps valide de référence : département 2 = celui de manager (poste « Manager informatique »), poste 3 de ce département
# Postes des comptes (base/data.sql) : 6 manager (Info), 7 hr (RH), 10 admin (Info)
DEP=2; MOTIF='"Test valeurs absurdes"'; DELAI="\"$(date_dans 30)\""; CRE='[{"idFonction":1,"effectif":2}]'; REM='[3]'

# Comme verifier, mais retient l'id de la demande si elle a été créée (pour la supprimer à la fin).
creer() {
  verifier "$@"
  if [ "${DERNIER_HTTP:-}" = "201" ]; then
    CREEES+=("$(grep -o '^{"id":[0-9]*' "$TMP_TESTS/corps" | grep -o '[0-9]*$')")
  fi
}

nettoyer() {
  [ ${#CREEES[@]} -eq 0 ] && return
  local ids; ids=$(IFS=,; echo "${CREEES[*]}")
  if PGPASSWORD="${PGPASSWORD:-postgres}" psql -q -h "${PGHOST:-localhost}" -U "${PGUSER:-postgres}" \
      -d "${PGDATABASE:-gestion_hr}" -v ON_ERROR_STOP=1 >/dev/null 2>&1 <<SQL
BEGIN;
DELETE FROM demandeFonction WHERE idDemande IN ($ids);
DELETE FROM demandePoste    WHERE idDemande IN ($ids);
DELETE FROM histoDemande    WHERE idDemande IN ($ids);
DELETE FROM demande         WHERE id IN ($ids);
COMMIT;
SQL
  then
    printf '\n%sDemandes de test supprimées : %s%s\n' "$GRIS" "$ids" "$FIN"
  else
    printf '\n%sImpossible de supprimer les demandes de test (psql) : %s%s\n' "$ROUGE" "$ids" "$FIN"
  fi
}

# ---------------------------------------------------------------------------
section "Demande — droits"
session_anonyme
verifier "créer sans être connecté"          401:NON_CONNECTE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "$CRE" "$REM")"
connecter hr hr123
creer    "créer en tant que HR"              403:ACCES_REFUSE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "$CRE" "$REM")"
connecter manager manager123
JETON= creer "créer sans jeton CSRF"         403 POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "$CRE" "$REM")"
creer    "corps valide (contrôle)"           201 POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "$CRE" "$REM")"
CONTROLE="${CREEES[-1]:-0}"

section "Demande — corps de la requête"
creer "JSON cassé"                           400:REQUETE_ILLISIBLE POST $DEMANDES '{"idDepartement":1,'
creer "tableau au lieu d'objet"              400:REQUETE_ILLISIBLE POST $DEMANDES '[]'
creer "objet vide {}"                        400:VALIDATION        POST $DEMANDES '{}'
creer "corps vide"                           400                   POST $DEMANDES '' -H 'Content-Type: application/json'
creer "texte brut (pas JSON)"                415                   POST $DEMANDES '' -H 'Content-Type: text/plain' --data 'bonjour'

section "Demande — département"
for v in null -1 0 999999 2147483647; do
  creer "idDepartement = $v"                 400:VALIDATION        POST $DEMANDES "$(corps "$v" "$MOTIF" "$DELAI" "$CRE" "$REM")"
done
creer "idDepartement = 99999999999 (> int)"  400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps 99999999999 "$MOTIF" "$DELAI" "$CRE" "$REM")"
creer "idDepartement = \"abc\""              400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps '"abc"' "$MOTIF" "$DELAI" "$CRE" "$REM")"
creer "idDepartement = true"                 400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps true "$MOTIF" "$DELAI" "$CRE" "$REM")"
creer "idDepartement = 1.9 (décimal)"        400                   POST $DEMANDES "$(corps 1.9 "$MOTIF" "$DELAI" "$CRE" "$REM")"

section "Demande — motif"
for v in null '""' '"   "' '"\n\t"'; do
  creer "motif = $v"                         400:VALIDATION        POST $DEMANDES "$(corps $DEP "$v" "$DELAI" "$CRE" "$REM")"
done
creer "motif de 201 caractères"              400:VALIDATION        POST $DEMANDES "$(corps $DEP "\"$(printf 'm%.0s' {1..201})\"" "$DELAI" "$CRE" "$REM")"
creer "motif de 100 000 caractères"          400:VALIDATION        POST $DEMANDES "$(corps $DEP "\"$(printf 'm%.0s' {1..100000})\"" "$DELAI" "$CRE" "$REM")"
creer "motif tableau"                        400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP '["a"]' "$DELAI" "$CRE" "$REM")"

section "Demande — délai (dates bizarres)"
for v in "$(date_dans -1)" "$(date_dans 0)" "$(date_dans 6)" 0000-01-01 1900-01-01; do
  creer "délai = $v (moins d'une semaine)"   400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "\"$v\"" "$CRE" "$REM")"
done
for v in 2027-02-30 2029-02-29 2027-13-01 2027-00-10 2027-01-32 2027-1-1 31/12/2027 12-31-2027 demain 'undefined'; do
  creer "délai = \"$v\" (date invalide)"     400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" "\"$v\"" "$CRE" "$REM")"
done
creer "délai = nombre 20270101"              400                   POST $DEMANDES "$(corps $DEP "$MOTIF" 20270101 "$CRE" "$REM")"
creer "délai = true"                         400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" true "$CRE" "$REM")"
creer "délai = date + heure"                 400                   POST $DEMANDES "$(corps $DEP "$MOTIF" '"2027-06-01T10:00:00"' "$CRE" "$REM")"
creer "délai = an +99999"                    400                   POST $DEMANDES "$(corps $DEP "$MOTIF" '"+99999-01-01"' "$CRE" "$REM")"
creer "délai = an 9999 (dans 7000 ans)"      400                   POST $DEMANDES "$(corps $DEP "$MOTIF" '"9999-12-31"' "$CRE" "$REM")"

section "Demande — postes à créer (fonction, effectif)"
creer "aucune création ni remplacement"      400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[]')"
creer "creations et remplacements null"      400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" null null)"
for v in 0 -1 -2147483648 null; do
  creer "effectif = $v"                      400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "[{\"idFonction\":1,\"effectif\":$v}]" '[]')"
done
creer "effectif absent"                      400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1}]' '[]')"
creer "effectif = 1.5 (décimal)"             400                   POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1,"effectif":1.5}]' '[]')"
creer "effectif = 2147483648 (> int)"        400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1,"effectif":2147483648}]' '[]')"
creer "effectif = 2147483647 (2 milliards)"  400                   POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1,"effectif":2147483647}]' '[]')"
creer "effectif = \"beaucoup\""              400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1,"effectif":"beaucoup"}]' '[]')"
for v in null -1 0 999999; do
  creer "idFonction = $v"                    400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" "[{\"idFonction\":$v,\"effectif\":1}]" '[]')"
done
creer "création null dans la liste"          400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[null]' '[]')"
creer "création {} vide"                     400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{}]' '[]')"
creer "même fonction deux fois"              400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[{"idFonction":1,"effectif":1},{"idFonction":1,"effectif":1}]' '[]')"
creer "creations objet au lieu de liste"     400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '{"idFonction":1,"effectif":1}' '[]')"

section "Demande — postes à remplacer"
for v in '[null]' '[-1]' '[0]' '[999999]'; do
  creer "remplacements = $v"                 400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' "$v")"
done
creer "poste d'un autre département (1)"     400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[1]')"
creer "même poste deux fois"                 400:VALIDATION        POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[3,3]')"
creer "remplacements = [\"abc\"]"            400:REQUETE_ILLISIBLE POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '["abc"]')"

section "Demande — son département, son poste, pas ceux des autres comptes"
creer "demande pour un autre département (1)" 403:ACCES_REFUSE    POST $DEMANDES "$(corps 1 "$MOTIF" "$DELAI" '[]' '[1]')"
creer "demande pour un autre département (3)" 403:ACCES_REFUSE    POST $DEMANDES "$(corps 3 "$MOTIF" "$DELAI" "$CRE" '[]')"
creer "remplacer le poste d'admin (10)"      403:ACCES_REFUSE     POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[10]')"
creer "remplacer le poste de hr (7, autre dép.)" 400:VALIDATION   POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[7]')"
creer "remplacer son propre poste (6)"       201                  POST $DEMANDES "$(corps $DEP "$MOTIF" "$DELAI" '[]' '[6]')"

section "Demande — valider / refuser (id dans l'URL)"
connecter hr hr123
for id in -1 0 2147483647; do
  verifier "valider la demande $id"          404:RESSOURCE_INTROUVABLE POST "$DEMANDES/$id/valider"
  verifier "refuser la demande $id"          404:RESSOURCE_INTROUVABLE POST "$DEMANDES/$id/refuser"
done
for id in abc 1.5 99999999999 '%20'; do
  verifier "valider la demande « $id »"      400 POST "$DEMANDES/$id/valider"
done
if [ "$CONTROLE" != "0" ]; then
  connecter direction direction123
  verifier "Direction valide avant HR"       403:ACCES_REFUSE POST "$DEMANDES/$CONTROLE/valider"
  connecter manager manager123
  verifier "le manager valide sa demande"    403:ACCES_REFUSE POST "$DEMANDES/$CONTROLE/valider"
  connecter hr hr123
  verifier "HR refuse (contrôle)"            200 POST "$DEMANDES/$CONTROLE/refuser"
  verifier "HR refuse une 2e fois"           409:TRANSITION_INTERDITE POST "$DEMANDES/$CONTROLE/refuser"
  verifier "HR valide une demande refusée"   409:TRANSITION_INTERDITE POST "$DEMANDES/$CONTROLE/valider"
fi

section "Demande — lecture"
verifier "détail de la demande 0"            404:RESSOURCE_INTROUVABLE GET "$DEMANDES/0"
verifier "détail de la demande 2147483647"   404:RESSOURCE_INTROUVABLE GET "$DEMANDES/2147483647"
verifier "détail de la demande -1"           4xx GET "$DEMANDES/-1"
verifier "détail de la demande « abc »"      4xx GET "$DEMANDES/abc"
verifier "détail de la demande 99999999999"  400 GET "$DEMANDES/99999999999"
verifier "toutes les demandes en tant que HR" 403:ACCES_REFUSE GET "$DEMANDES"

nettoyer
bilan
