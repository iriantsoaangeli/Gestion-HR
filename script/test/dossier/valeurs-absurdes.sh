#!/usr/bin/env bash
# Dossiers de candidature : valeurs absurdes sur /api/dossiers (dépôt multipart, lecture). Contrat : doc/routes/api/dossiers.md.
# S'appuie sur la démo du recrutement (base/demo.sql) : offre « Comptable » ouverte, « Gestionnaire paie » close,
# Andry a déjà un dossier pour « Comptable », Haja non. Chaque cas part d'un dépôt valide et ne change qu'un champ.
# Les ids sont lus avec psql, et tout ce qui a été créé (dossiers, pièces jointes, personnes) est supprimé à la fin
# (PGHOST, PGUSER, PGPASSWORD, PGDATABASE ; valeurs par défaut = backend/src/main/resources/application.properties).
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

D=/api/dossiers
DOSSIERS=()
PERSONNES=()

sql() {
  PGPASSWORD="${PGPASSWORD:-postgres}" psql -tA -h "${PGHOST:-localhost}" -U "${PGUSER:-postgres}" \
    -d "${PGDATABASE:-gestion_hr}" -v ON_ERROR_STOP=1 -c "$1"
}

# Champ de la réponse du dernier appel ({"id":N,...,"idPersonne":M,...})
champ() { grep -o "\"$1\":[0-9]*" "$TMP_TESTS/corps" | head -1 | grep -o '[0-9]*$'; }

OUVERTE=$(sql "SELECT id FROM offre WHERE intitule = 'Comptable' AND status = 'OUVERT'")
CLOSE=$(sql "SELECT id FROM offre WHERE status = 'CLOS' LIMIT 1")
ANDRY=$(sql "SELECT id FROM personne WHERE email = 'andry.rakotoarisoa@exemple.mg'")
HAJA=$(sql "SELECT id FROM personne WHERE email = 'haja.andriamanana@exemple.mg'")
if [ -z "$OUVERTE" ] || [ -z "$CLOSE" ] || [ -z "$ANDRY" ] || [ -z "$HAJA" ]; then
  echo "${ROUGE}Démo du recrutement absente (base/demo.sql) ou psql injoignable.${FIN}" >&2
  exit 2
fi

nettoyer() {
  [ ${#DOSSIERS[@]} -eq 0 ] && [ ${#PERSONNES[@]} -eq 0 ] && return
  local ids pers
  ids=$(IFS=,; echo "${DOSSIERS[*]:-0}"); pers=$(IFS=,; echo "${PERSONNES[*]:-0}")
  if sql "BEGIN;
CREATE TEMP TABLE zz_pj ON COMMIT DROP AS SELECT idPieceJointe AS id FROM pieceJointeDossier WHERE idDossier IN ($ids);
DELETE FROM pieceJointeDossier WHERE idDossier IN ($ids);
DELETE FROM pieceJointe WHERE id IN (SELECT id FROM zz_pj);
DELETE FROM dossier WHERE id IN ($ids);
DELETE FROM personne WHERE id IN ($pers);
COMMIT;" >/dev/null 2>&1; then
    printf '\n%sDossiers de test supprimés : %s ; personnes créées : %s%s\n' "$GRIS" "$ids" "$pers" "$FIN"
  else
    printf '\n%sImpossible de supprimer les données de test (psql), dossiers : %s%s\n' "$ROUGE" "$ids" "$FIN"
  fi
}
trap 'nettoyer; rm -rf "$TMP_TESTS"' EXIT

# Fichiers de test
CV="$TMP_TESTS/cv.txt"; echo "CV de test" > "$CV"
VIDE="$TMP_TESTS/vide.txt"; : > "$VIDE"
GROS="$TMP_TESTS/gros.bin"; truncate -s 31M "$GROS"
MOITIE="$TMP_TESTS/moitie.bin"; truncate -s 16M "$MOITIE"

# Nouvelle personne valide : -F de chaque champ (email unique à chaque lancement)
# (--form-string et pas -F : avec -F, une valeur qui commence par @ ou < serait lue comme un fichier)
NOUVELLE=(--form-string nom=Testeur --form-string prenom=Zaza --form-string naissance=1990-05-05
  --form-string "adresse=Lot test" --form-string "tel=034 00 000 00" --form-string "email=zaza.$(date +%s)@exemple.mg")
# nouvelle CHAMP VALEUR : la nouvelle personne valide avec un champ changé
nouvelle() {
  local args=() i
  for i in "${NOUVELLE[@]}"; do
    [[ "$i" == "$1="* ]] && i="$1=$2"
    args+=("$i")
  done
  printf '%s\0' "${args[@]}"
}
deposer() { local t="$1" a="$2"; shift 2; verifier "$t" "$a" POST $D "" "$@"; }

# ---------------------------------------------------------------------------
section "Dossier — droits"
session_anonyme
verifier "lister sans être connecté"           401:NON_CONNECTE GET $D
deposer  "déposer sans être connecté"          401:NON_CONNECTE -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV"
connecter manager manager123
verifier "lister en tant que manager"          403:ACCES_REFUSE GET $D
verifier "personnes en tant que manager"       403:ACCES_REFUSE GET "$D/personnes"
verifier "pièce jointe en tant que manager"    403:ACCES_REFUSE GET "$D/pieces-jointes/1"
deposer  "déposer en tant que manager"         403:ACCES_REFUSE -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV"
connecter direction direction123
verifier "lister en tant que Direction"        403:ACCES_REFUSE GET $D
connecter hr hr123
JETON= deposer "déposer sans jeton CSRF"       403 -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV"

section "Dossier — corps de la requête"
verifier "JSON au lieu de multipart"           4xx POST $D '{"idOffre":1}'
deposer  "multipart sans aucun champ"          400:VALIDATION -F x=1

section "Dossier — offre"
deposer  "sans offre"                          400:VALIDATION -F idPersonne=$HAJA -F fichiers=@"$CV"
for v in '' '   ' abc -1 0 999999 99999999999 1.5; do
  deposer "idOffre = « $v »"                   400:VALIDATION -F "idOffre=$v" -F idPersonne=$HAJA -F fichiers=@"$CV"
done
deposer  "offre close"                         400:VALIDATION -F idOffre=$CLOSE -F idPersonne=$HAJA -F fichiers=@"$CV"

section "Dossier — personne de la base"
deposer  "ni personne ni nouvelle"             400:VALIDATION -F idOffre=$OUVERTE -F fichiers=@"$CV"
for v in abc -1 0 999999 99999999999; do
  deposer "idPersonne = « $v »"                400:VALIDATION -F idOffre=$OUVERTE -F "idPersonne=$v" -F fichiers=@"$CV"
done
deposer  "dossier déjà déposé (Andry)"         409:DEJA_FAIT  -F idOffre=$OUVERTE -F idPersonne=$ANDRY -F fichiers=@"$CV"
deposer  "personne de la base ET nouvelle"     400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA "${NOUVELLE[@]}" -F fichiers=@"$CV"

section "Dossier — nouvelle personne"
for c in nom prenom naissance adresse tel email; do
  for v in '' '   '; do
    mapfile -d '' args < <(nouvelle "$c" "$v")
    deposer "$c = « $v »"                      400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
  done
done
mapfile -d '' args < <(nouvelle nom "$(printf 'n%.0s' {1..101})")
deposer  "nom de 101 caractères"               400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
mapfile -d '' args < <(nouvelle adresse "$(printf 'a%.0s' {1..201})")
deposer  "adresse de 201 caractères"           400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
for v in abc 12 '034 abc 00' '0340000000000000000000'; do
  mapfile -d '' args < <(nouvelle tel "$v")
  deposer "tel = « $v »"                       400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
done
for v in abc 'a@b' '@exemple.mg' 'a b@exemple.mg'; do
  mapfile -d '' args < <(nouvelle email "$v")
  deposer "email = « $v »"                     400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
done
for v in "$(date_dans 0)" "$(date_dans 30)" 1899-12-31 2000-02-30 31/12/1990 hier; do
  mapfile -d '' args < <(nouvelle naissance "$v")
  deposer "naissance = « $v »"                 400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
done
mapfile -d '' args < <(nouvelle email 'ANDRY.rakotoarisoa@exemple.mg')
deposer  "e-mail déjà dans la base (casse)"    400:VALIDATION -F idOffre=$OUVERTE "${args[@]}" -F fichiers=@"$CV"
deposer  "même nom + prénom + naissance (sans accents)" 400:VALIDATION -F idOffre=$OUVERTE -F nom=RANDRIAMAMPIONONA \
  -F prenom=eric -F naissance=1991-10-19 -F "adresse=Lot test" -F "tel=034 00 000 01" -F email=autre.eric@exemple.mg -F fichiers=@"$CV"

section "Dossier — pièces jointes"
deposer  "aucun fichier"                       400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA
deposer  "fichier vide"                        400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$VIDE"
deposer  "même fichier deux fois"              400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV" -F fichiers=@"$CV"
deposer  "même nom, casse différente"          400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV" \
  -F "fichiers=@$CV;filename=CV.TXT"
deposer  "nom de fichier de 256 caractères"    400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA \
  -F "fichiers=@$CV;filename=$(printf 'f%.0s' {1..252}).txt"
deposer  "un fichier de 31 Mo"                 400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$GROS"
deposer  "deux fichiers de 16 Mo (32 Mo)"      400:VALIDATION -F idOffre=$OUVERTE -F idPersonne=$HAJA \
  -F "fichiers=@$MOITIE;filename=a.bin" -F "fichiers=@$MOITIE;filename=b.bin"

section "Dossier — dépôts (contrôle)"
deposer  "Haja, personne de la base (contrôle)" 201 -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV" \
  -F "fichiers=@$CV;filename=Lettre.txt"
DOSSIER="$(champ id)"; [ -n "$DOSSIER" ] && DOSSIERS+=("$DOSSIER")
PJ="$(grep -o '"piecesJointes":\[{"id":[0-9]*' "$TMP_TESTS/corps" | grep -o '[0-9]*$')"
deposer  "Haja une deuxième fois"              409:DEJA_FAIT  -F idOffre=$OUVERTE -F idPersonne=$HAJA -F fichiers=@"$CV"
deposer  "nouvelle personne (contrôle)"        201 -F idOffre=$OUVERTE "${NOUVELLE[@]}" -F "fichiers=@$CV;filename=dossier/sous/cv.txt"
d="$(champ id)"; p="$(champ idPersonne)"
[ -n "$d" ] && DOSSIERS+=("$d"); [ -n "$p" ] && PERSONNES+=("$p")
if grep -q '"fichiers":\["cv.txt"\]' "$TMP_TESTS/corps"; then
  REUSSIS=$((REUSSIS + 1)); echo "  ${VERT}OK${FIN}    chemin retiré du nom de fichier (cv.txt)"
else
  ECHECS=$((ECHECS + 1)); echo "  ${ROUGE}ECHEC${FIN} nom de fichier : $(head -c 300 "$TMP_TESTS/corps")"
fi
deposer  "la même nouvelle personne recréée"   400:VALIDATION -F idOffre=$OUVERTE "${NOUVELLE[@]}" -F fichiers=@"$CV"

section "Dossier — lecture"
verifier "tous les dossiers"                   200 GET $D
verifier "dossiers de l'offre ouverte"         200 GET "$D?offre=$OUVERTE"
verifier "dossiers de l'offre 999999"          200 GET "$D?offre=999999"
verifier "dossiers de l'offre « abc »"         4xx GET "$D?offre=abc"
verifier "personnes"                           200 GET "$D/personnes"
[ -n "$PJ" ] && verifier "télécharger la pièce jointe déposée" 200 GET "$D/pieces-jointes/$PJ"
verifier "pièce jointe 0"                      404:RESSOURCE_INTROUVABLE GET "$D/pieces-jointes/0"
verifier "pièce jointe 2147483647"             404:RESSOURCE_INTROUVABLE GET "$D/pieces-jointes/2147483647"
verifier "pièce jointe « abc »"                4xx GET "$D/pieces-jointes/abc"
verifier "pièce jointe -1"                     4xx GET "$D/pieces-jointes/-1"

bilan
