#!/usr/bin/env bash
# Auth : valeurs absurdes sur /api/auth (login, session, CSRF). Ne modifie pas la base.
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../lib.sh"
backend_disponible

LOGIN=/api/auth/login

section "Auth — corps de la connexion"
session_anonyme
verifier "login et mdp vides"                401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"","mdp":""}'
verifier "login et mdp null"                 401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":null,"mdp":null}'
verifier "objet vide {}"                     401:IDENTIFIANTS_INVALIDES POST $LOGIN '{}'
verifier "login seulement des espaces"       401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"   ","mdp":"manager123"}'
verifier "bon login, mauvais mdp"            401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"manager","mdp":"faux"}'
verifier "bon login, mdp vide"               401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"manager","mdp":""}'
verifier "login inexistant"                  401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"personne","mdp":"x"}'
verifier "login en majuscules (MANAGER)"     401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":"MANAGER","mdp":"manager123"}'
verifier "injection SQL dans le login"       401:IDENTIFIANTS_INVALIDES POST $LOGIN "{\"login\":\"' OR '1'='1' --\",\"mdp\":\"x\"}"
verifier "injection SQL dans le mdp"         401:IDENTIFIANTS_INVALIDES POST $LOGIN "{\"login\":\"manager\",\"mdp\":\"' OR '1'='1\"}"
verifier "login de 10 000 caractères"        401:IDENTIFIANTS_INVALIDES POST $LOGIN "{\"login\":\"$(printf 'a%.0s' {1..10000})\",\"mdp\":\"x\"}"
verifier "login nombre au lieu de texte"     401:IDENTIFIANTS_INVALIDES POST $LOGIN '{"login":123,"mdp":456}'
verifier "login tableau"                     400:REQUETE_ILLISIBLE      POST $LOGIN '{"login":["manager"],"mdp":"manager123"}'
verifier "login objet"                       400:REQUETE_ILLISIBLE      POST $LOGIN '{"login":{"a":1},"mdp":"manager123"}'
verifier "JSON cassé"                        400:REQUETE_ILLISIBLE      POST $LOGIN '{"login":"manager",'
verifier "corps qui n'est pas du JSON"       400:REQUETE_ILLISIBLE      POST $LOGIN 'login=manager&mdp=manager123'
verifier "tableau au lieu d'objet"           400:REQUETE_ILLISIBLE      POST $LOGIN '[]'
verifier "corps vide"                        400                        POST $LOGIN '' -H 'Content-Type: application/json'

section "Auth — CSRF"
session_anonyme
JETON= verifier "login sans en-tête X-XSRF-TOKEN" 403 POST $LOGIN '{"login":"manager","mdp":"manager123"}'
JETON=00000000-0000-0000-0000-000000000000 \
  verifier "login avec un jeton CSRF inventé"   403 POST $LOGIN '{"login":"manager","mdp":"manager123"}'

section "Auth — session"
session_anonyme
verifier "/moi sans être connecté"           401:NON_CONNECTE GET /api/auth/moi
verifier "/moi avec un JSESSIONID inventé"   401:NON_CONNECTE GET /api/auth/moi '' -b 'JSESSIONID=ABCDEF0123456789'
connecter manager manager123
verifier "/moi connecté (contrôle)"          200 GET /api/auth/moi
verifier "logout"                            204 POST /api/auth/logout
verifier "/moi après logout"                 401:NON_CONNECTE GET /api/auth/moi

bilan
