# API — Demandes de personnel (`/api/demandes`)

Connexion obligatoire (session, voir `doc/user.md`). Les POST envoient l'en-tête `X-XSRF-TOKEN`.
Le demandeur et le valideur ne sont **jamais** dans le JSON : c'est l'utilisateur connecté.

## Workflow

`EN_ATTENTE_HR` → `EN_ATTENTE_DIRECTION` → `EN_ATTENTE_FINANCE` → `APPROUVEE`, ou `REFUSEE` à n'importe quelle étape.

| Statut (id) | Qui décide |
|---|---|
| `EN_ATTENTE_HR` (1) | rôle `HR` |
| `EN_ATTENTE_DIRECTION` (2) | rôle `DIRECTION` |
| `EN_ATTENTE_FINANCE` (3) | rôle `FINANCE` |
| `APPROUVEE` (4), `REFUSEE` (5) | personne : terminée |

Règles dans `demande/Model/Statut.java` (enum, ids = table `status`) et `demande/Service/DemandeService.java`.

## Routes

| Méthode | Route | Qui | Réponse |
|---|---|---|---|
| POST | `/api/demandes` | `MANAGER` | 201 `DemandeResponse` (statut `EN_ATTENTE_HR`) |
| POST | `/api/demandes/{id}/valider` | rôle de l'étape | `DemandeResponse` (statut suivant) |
| POST | `/api/demandes/{id}/refuser` | rôle de l'étape | `DemandeResponse` (statut `REFUSEE`) |
| GET | `/api/demandes/mes-demandes` | tous | demandes que j'ai créées |
| GET | `/api/demandes/traitees` | tous | demandes sur lesquelles j'ai validé ou refusé |
| GET | `/api/demandes/a-traiter` | tous (vide si mon rôle ne valide pas) | demandes qui attendent ma décision, les plus anciennes d'abord |
| GET | `/api/demandes` | `ADMIN`, `DIRECTION` | toutes les demandes (403 sinon) |
| GET | `/api/demandes/{id}` | demandeur, ceux qui ont décidé, rôle dont c'est le tour, `ADMIN`, `DIRECTION` | une demande (403 sinon, 404 inconnue) |
| GET | `/api/demandes/{id}/validateurs` | mêmes droits que la fiche | personnel ayant **validé** (sans les refus) : `[{ date, statut, personnel }]` |
| GET | `/api/demandes/etapes` | tous | étapes du workflow (progression du formulaire), même forme que `suivi` |

## Référentiel (`/api/referentiel`, tout utilisateur connecté) → `[{ id, label }]`

| Route | Contenu |
|---|---|
| `GET /api/referentiel/departements` | départements |
| `GET /api/referentiel/mon-departement` | `{ id, label }` : département du compte connecté (celui de son poste) |
| `GET /api/referentiel/fonctions` | fonctions (pour les créations) |
| `GET /api/referentiel/postes?departement={id}` | postes du département que **je** peux faire remplacer : sans les postes des autres comptes, avec le mien |

### Créer

```json
{
  "idDepartement": 2,
  "motif": "Nouveau projet web",
  "delai": "2027-02-02",
  "creations": [{ "idFonction": 3, "effectif": 2 }],
  "remplacements": [4]
}
```

- `creations` : postes à **créer**. Le poste n'existe pas encore : on désigne sa **fonction** et l'**effectif** (≥ 1). → table `demandeFonction`.
- `remplacements` : id de postes **existants** du département, sans effectif (1 poste = 1 siège). → table `demandePoste`.
- `idDepartement` : **le département du manager** (celui de son poste, voir `mon-departement`), sinon 403 `ACCES_REFUSE`.
- Un manager peut demander **son propre remplacement**, mais pas celui d'un poste tenu par un autre compte
  (autres managers, supérieurs) : 403 `ACCES_REFUSE`.
- Au moins un poste en tout. `delai` facultatif, au moins 7 jours après aujourd'hui. `motif` obligatoire, 200 caractères max.

### `DemandeResponse`

```json
{
  "id": 1, "idDepartement": 2, "departement": "Informatique",
  "statut": "EN_ATTENTE_DIRECTION", "statutLibelle": "En attente Direction", "etapeValideur": "DIRECTION",
  "idDemandeur": 1, "demandeur": "manager",
  "date": "2026-11-11", "motif": "Nouveau projet web", "delai": "2027-02-02",
  "creations": [{ "idFonction": 3, "fonction": "Développeur", "effectif": 2 }],
  "remplacements": [{ "idPoste": 4, "poste": "Administrateur système" }],
  "historique": [{ "utilisateur": "hr", "role": "HR", "statut": "EN_ATTENTE_DIRECTION", "date": "2026-11-12" }],
  "aMoiDeDecider": false,
  "suivi": [
    { "libelle": "Manager", "etat": "faite", "texte": "Demande créée", "date": "2026-11-11", "utilisateur": "manager",
      "personnel": [{ "idUtilisateur": 1, "login": "manager", "role": "MANAGER", "roleLibelle": "Manager",
                      "idPoste": 6, "poste": "Manager informatique", "detenteur": "Hery Rakotomalala" }] },
    { "libelle": "HR", "etat": "faite", "texte": "Validée", "date": "2026-11-12", "utilisateur": "hr", "personnel": [ … ] },
    { "libelle": "Direction", "etat": "courante", "texte": "En attente depuis", "date": "2026-11-12", "utilisateur": null, "personnel": [ … ] },
    { "libelle": "Finance", "etat": "a-venir", "texte": "À venir", "date": null, "utilisateur": null, "personnel": [ … ] }
  ]
}
```

`etapeValideur` est `null` quand la demande est terminée. `aMoiDeDecider` : c'est à l'utilisateur connecté de valider / refuser.
`personnel` d'une étape : le **détenteur du poste** (`detenteur` = prénom nom de `poste.idPersonne`, null si vacant), son poste et son rôle — celui qui a créé / décidé si l'étape est faite, sinon les comptes du rôle qui décidera.

`suivi` est calculé par le serveur (`etat` : `faite`, `courante`, `refusee`, `a-venir`) : le front l'affiche tel quel (barre de progression et bloc Suivi). `historique` = une ligne par décision (table `histoDemande`), `statut` = statut obtenu.

## Erreurs (`{ "code", "message" }`)

| Statut | Code | Quand |
|---|---|---|
| 400 | `VALIDATION` | département / fonction / poste inconnu, poste d'un autre département, doublon, effectif < 1, aucun poste, motif vide ou trop long, délai < 1 semaine |
| 400 | `REQUETE_ILLISIBLE` | JSON invalide |
| 401 | `NON_CONNECTE` | pas de session |
| 403 | `ACCES_REFUSE` | créer sans être `MANAGER`, décider à une étape qui n'est pas la sienne, voir une demande qui ne me concerne pas, liste complète sans être `ADMIN`/`DIRECTION`, jeton CSRF absent |
| 404 | `RESSOURCE_INTROUVABLE` | demande inconnue |
| 409 | `TRANSITION_INTERDITE` | demande déjà approuvée ou refusée |

## Code

`backend/src/main/java/gestion/hr/` (une fonctionnalité = un dossier, puis une couche par sous-dossier) :
- `demande/` : `Controller/DemandeController` → `Service/DemandeService` (seul point d'entrée, transactions), qui appelle
  les classes du même package, invisibles ailleurs : `CreationDemande`, `DecisionDemande`, `LectureDemande`,
  `ConversionDemande` (Demande → `DemandeResponse`, suivi). `Model/` (Demande, DemandeFonction, DemandePoste,
  HistoDemande, Statut, …), `Repository/`, `Dto/`.
- `referentiel/` : `ReferentielController` → `ReferentielService` (listes, postes remplaçables, personnel).
- `auth/` (connexion, comptes), `commun/Exception/` (erreurs JSON).
Tests : `src/test/java/gestion/hr/demande/Controller/DemandeControllerTest` (sur la base locale, annulés à la fin).

## Front

- Appels : `frontend/src/api/demandes.js`, `api/referentiel.js` (client commun `api/client.js`).
- Pages : Mes demandes (`/mes-demandes`, accueil), Nouvelle demande (`/demande`, MANAGER), Demandes à traiter
  (`/demandes-a-traiter` : à valider + déjà traitées par moi, HR/DIRECTION/FINANCE), Toutes (`/toutes-les-demandes`,
  ADMIN/DIRECTION), fiche (`/demandes/{id}`) avec le panneau Valider / Refuser quand `aMoiDeDecider`.
