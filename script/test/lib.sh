#!/usr/bin/env bash
# Outils communs aux tests d'API (à « sourcer », pas à lancer).
# Chaque test envoie une requête curl et compare le code HTTP (et le champ "code" du JSON si précisé).
#
#   session_anonyme              nouvelle session sans connexion (cookie XSRF-TOKEN seulement)
#   connecter LOGIN MDP          nouvelle session connectée
#   verifier "titre" ATTENDU METHODE CHEMIN [CORPS_JSON] [OPTIONS_CURL...]
#       ATTENDU = 400 | 400:VALIDATION | 4xx   (code HTTP, ou code HTTP + champ "code" du JSON)
#       JETON=valeur verifier ...  force le jeton CSRF (JETON= verifier ... : sans jeton)
#   bilan                        affiche le résumé, code de sortie 1 s'il y a un échec

API="${API:-http://localhost:8080}"
TMP_TESTS="$(mktemp -d)"
trap 'rm -rf "$TMP_TESTS"' EXIT

REUSSIS=0
ECHECS=0
JAR=""

VERT=$'\e[32m'; ROUGE=$'\e[31m'; GRIS=$'\e[90m'; GRAS=$'\e[1m'; FIN=$'\e[0m'
[ -t 1 ] || { VERT=""; ROUGE=""; GRIS=""; GRAS=""; FIN=""; }

section() { printf '\n%s== %s ==%s\n' "$GRAS" "$1" "$FIN"; }

# Vérifie que le backend répond, sinon arrête tout.
backend_disponible() {
  if ! curl -s -o /dev/null --max-time 5 "$API/api/auth/csrf"; then
    echo "${ROUGE}Backend injoignable sur $API (lancer le backend, ou API=http://hote:port).${FIN}" >&2
    exit 2
  fi
}

# Valeur du cookie XSRF-TOKEN de la session courante.
jeton_csrf() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$JAR" 2>/dev/null | tail -1; }

session_anonyme() {
  JAR="$(mktemp -p "$TMP_TESTS" jar.XXXX)"
  curl -s -o /dev/null -c "$JAR" -b "$JAR" "$API/api/auth/csrf"
}

connecter() {
  session_anonyme
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' -c "$JAR" -b "$JAR" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(jeton_csrf)" \
    -X POST "$API/api/auth/login" --data "{\"login\":\"$1\",\"mdp\":\"$2\"}")
  if [ "$code" != "200" ]; then
    echo "${ROUGE}Connexion de « $1 » impossible (HTTP $code) : vérifier base/data.sql.${FIN}" >&2
    exit 2
  fi
  # Le jeton CSRF change à la connexion : on le relit
  curl -s -o /dev/null -c "$JAR" -b "$JAR" "$API/api/auth/csrf"
}

verifier() {
  local titre="$1" attendu="$2" methode="$3" chemin="$4" corps="${5-}"
  shift 4; [ $# -gt 0 ] && shift
  local args=(-s -o "$TMP_TESTS/corps" -w '%{http_code}' -X "$methode" -c "$JAR" -b "$JAR")
  # JETON=... force le jeton CSRF envoyé (JETON= : aucun jeton)
  local jeton; if [ -n "${JETON+x}" ]; then jeton="$JETON"; else jeton="$(jeton_csrf)"; fi
  [ -n "$jeton" ] && args+=(-H "X-XSRF-TOKEN: $jeton")
  [ -n "$corps" ] && args+=(-H 'Content-Type: application/json' --data-binary "$corps")

  local http
  http=$(curl "${args[@]}" "$@" "$API$chemin")
  DERNIER_HTTP="$http"   # lu par les scripts qui ont besoin de la réponse (corps : $TMP_TESTS/corps)
  local recu_code
  recu_code=$(grep -o '"code" *: *"[A-Z_]*"' "$TMP_TESTS/corps" | head -1 | sed 's/.*"\([A-Z_]*\)"$/\1/')

  local http_attendu="${attendu%%:*}" code_attendu=""
  [[ "$attendu" == *:* ]] && code_attendu="${attendu#*:}"

  local ok=1
  if [ "$http_attendu" = "4xx" ]; then
    [[ "$http" == 4?? ]] || ok=0
  else
    [ "$http" = "$http_attendu" ] || ok=0
  fi
  [ -n "$code_attendu" ] && [ "$recu_code" != "$code_attendu" ] && ok=0

  if [ $ok -eq 1 ]; then
    REUSSIS=$((REUSSIS + 1))
    printf '  %sOK%s    %s %s(%s)%s\n' "$VERT" "$FIN" "$titre" "$GRIS" "$http${recu_code:+ $recu_code}" "$FIN"
  else
    ECHECS=$((ECHECS + 1))
    printf '  %sECHEC%s %s\n        attendu %s, reçu %s\n        %s %s\n        réponse : %s\n' \
      "$ROUGE" "$FIN" "$titre" "$attendu" "$http${recu_code:+:$recu_code}" "$methode" "$chemin" \
      "$(head -c 300 "$TMP_TESTS/corps")"
  fi
}

# Date au format AAAA-MM-JJ, décalée de N jours par rapport à aujourd'hui (GNU date).
date_dans() { date -d "$1 days" +%F; }

bilan() {
  printf '\n%s%d réussi(s), %d échec(s)%s\n' "$GRAS" "$REUSSIS" "$ECHECS" "$FIN"
  [ "$ECHECS" -eq 0 ]
}
