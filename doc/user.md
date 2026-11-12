# Utilisateurs et authentification

Comment on se connecte à l'application, quels comptes existent, et comment le code est organisé.
Connexion par **session + cookie** avec **Spring Security** ; tout le code d'authentification est dans `auth`
(backend `gestion.hr.auth`, front `frontend/src/auth/`).

## Table `utilisateur`

```
utilisateur(id, login, mdp, role)
```

| Colonne | Contenu |
|---|---|
| `login` | identifiant de connexion, unique (50 caractères max) |
| `mdp` | **hachage BCrypt** du mot de passe, jamais le mot de passe en clair |
| `role` | un des rôles ci-dessous, en texte |

Création : `base/init.sql`. Comptes de test : `base/data.sql`.

## Rôles

Définis dans `auth/Model/Role.java` (enum) : c'est la **seule source** des codes et des libellés.
Le front n'en garde aucune copie, il affiche le `roleLibelle` renvoyé par l'API.

| Code | Libellé | Dans le workflow d'une demande |
|---|---|---|
| `MANAGER` | Manager | crée la demande |
| `HR` | HR | 1re validation |
| `DIRECTION` | Direction | 2e validation |
| `FINANCE` | Finance | dernière validation |
| `ADMIN` | Administrateur | administration |

Côté Spring Security, chaque rôle devient l'autorité `ROLE_<code>` (ex. `ROLE_HR`).

**Menu selon le rôle** (calculé par le backend, jamais en dur dans le front) : `peutCreerDemande` (MANAGER) →
« Nouvelle demande » et « Mes demandes » ; `valideDesDemandes` (HR, DIRECTION, FINANCE, d'après `Statut`) → « Demandes à traiter ».

**Toutes les demandes** : seuls `ADMIN` et `DIRECTION` les voient (`Role.voitToutesLesDemandes()`),
les autres ne voient que les leurs. L'API renvoie ce droit dans `voitToutesLesDemandes` ; le front masque
le lien et renvoie vers l'accueil si on tape l'adresse `/toutes-les-demandes`.

**Accueil selon le rôle** (`/`, `router/index.js`) : MANAGER → Mes demandes ; HR, DIRECTION, FINANCE →
Demandes à traiter ; ADMIN → Toutes les demandes.

Le backend refuse aussi (403) la liste complète `GET /api/demandes` aux autres rôles : voir `doc/routes/api/demandes.md`.

## Comptes de test

Mots de passe **de développement uniquement**.

| Login | Mot de passe | Rôle |
|---|---|---|
| manager | manager123 | MANAGER |
| hr | hr123 | HR |
| direction | direction123 | DIRECTION |
| finance | finance123 | FINANCE |
| admin | admin123 | ADMIN |
| manager.rh, manager.finance | manager123 | MANAGER (données de démo, `base/demo.sql`) |

Remettre la base à zéro (supprime tout, recrée, charge init + data + demo ; `--sans-demo`, `-y` sans confirmation) :

```bash
base/reset.sh
```

Ou charger la base à la main :

```bash
psql -U postgres -d gestion_hr -f base/init.sql -f base/data.sql
# Facultatif : demandes de démonstration dans tous les statuts (une seule fois)
psql -U postgres -d gestion_hr -f base/demo.sql
```

## API (`/api/auth`)

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/api/auth/csrf` | — | pose le cookie `XSRF-TOKEN` (à appeler avant le premier POST) |
| POST | `/api/auth/login` | `{ "login", "mdp" }` | 200 `{ id, login, role, roleLibelle, voitToutesLesDemandes, peutCreerDemande, valideDesDemandes }` + cookie `JSESSIONID` |
| GET | `/api/auth/moi` | — | 200 utilisateur connecté |
| POST | `/api/auth/logout` | — | 204, session fermée |

Erreurs, toujours en JSON `{ "code", "message" }` :

| Statut | Code | Quand |
|---|---|---|
| 401 | `IDENTIFIANTS_INVALIDES` | login inconnu ou mauvais mot de passe (même message dans les deux cas) |
| 401 | `NON_CONNECTE` | appel d'une route protégée sans session |
| 403 | `ACCES_REFUSE` | jeton CSRF absent ou invalide |

Toutes les routes `/api/**` exigent d'être connecté, sauf `/api/auth/login` et `/api/auth/csrf`.

### Exemple avec curl

```bash
curl -c jar -b jar http://localhost:8080/api/auth/csrf
TOKEN=$(awk '/XSRF-TOKEN/{print $7}' jar)
curl -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" -H 'Content-Type: application/json' \
     -d '{"login":"hr","mdp":"hr123"}' http://localhost:8080/api/auth/login
curl -b jar http://localhost:8080/api/auth/moi
```

## Fonctionnement

1. Le front lit le cookie `XSRF-TOKEN` (ou appelle `/api/auth/csrf` s'il n'existe pas) et l'envoie dans l'en-tête `X-XSRF-TOKEN` sur chaque POST.
2. `POST /login` : `AuthenticationManager` vérifie le mot de passe avec BCrypt. Si c'est bon, l'id de session est changé (contre la fixation de session) et l'utilisateur est enregistré dans la session.
3. Les requêtes suivantes envoient le cookie `JSESSIONID` : Spring retrouve l'utilisateur dans la session.
4. La session expire après **30 minutes** d'inactivité. Le cookie est `HttpOnly` et `SameSite=Lax`.
5. `POST /logout` ferme la session et efface le jeton CSRF.

## Code

### Backend : `backend/src/main/java/gestion/hr/auth/`

```
auth/
├── Config/       SecurityConfig (règles d'accès, CSRF, session, BCrypt, logout)
│                 AuthErreurs (réponses 401 / 403 en JSON)
├── Controller/   AuthController (/login, /moi, /csrf)
├── Dto/          LoginRequest, UtilisateurResponse (le mot de passe ne sort jamais)
├── Model/        Utilisateur (entité JPA de la table utilisateur), Role (enum des rôles + libellés)
├── Repository/   UtilisateurRepository (findByLogin)
└── Service/      UtilisateurDetailsService (charge un compte pour Spring Security)
```

Tests : `backend/src/test/java/gestion/hr/auth/Controller/` (`AuthControllerTest`, `CsrfCookieTest`).
`HrApplicationTests` démarre toute l'application : il faut que la base locale tourne.

### Front : `frontend/src/auth/`

| Fichier | Rôle |
|---|---|
| `api.js` | `fetch` vers l'API (cookies + en-tête CSRF), `login`, `logout`, `moi`, classe `ErreurApi` |
| `session.js` | utilisateur connecté (`utilisateur`), `chargerSession`, `seConnecter`, `seDeconnecter` |

- Le routeur (`router/index.js`) renvoie vers `/login` toute page sans session, sauf celles marquées `meta: { public: true }`. Après connexion, on revient à la page demandée (`?redirect=`).
- Le menu (`Sidebar.vue`) affiche le login, le rôle et le bouton « Se déconnecter ».
- En dev, Vite redirige `/api` vers `http://localhost:8080` (`vite.config.js`) : le navigateur voit une seule origine, donc pas de CORS.

## Ajouter un compte

Le mot de passe doit être haché en BCrypt avant l'insertion (format `$2a$10$…`), par exemple avec
`new BCryptPasswordEncoder().encode("motdepasse")` dans un petit programme Java du projet.

```sql
INSERT INTO utilisateur (login, mdp, role)
VALUES ('nouveau', '$2a$10$...hachage...', 'MANAGER');
```

Le rôle doit être exactement un des codes de `Role.java`, sinon le compte ne peut pas être chargé.

## Configuration

`backend/src/main/resources/application.properties` :

| Propriété | Valeur |
|---|---|
| base | `jdbc:postgresql://localhost:5432/gestion_hr`, utilisateur `postgres` / `postgres` |
| `spring.jpa.hibernate.ddl-auto` | `validate` (Hibernate vérifie le schéma, ne le modifie jamais) |
| `server.servlet.session.timeout` | `30m` |

Lancer : `cd backend && mvn spring-boot:run` (le wrapper `./mvnw` ne marche pas : le dossier `.mvn/` manque), puis `script/run/run-front.sh`.
