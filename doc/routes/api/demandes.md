# Contrat API — Demandes de personnel

Connexion obligatoire (session + cookie, voir `doc/user.md`). Tout POST envoie l'en-tête `X-XSRF-TOKEN`.
Le demandeur et le valideur ne sont jamais dans le JSON : c'est l'utilisateur connecté.

Dates : `date` = date-heure `"2026-11-11T10:23:45"`, `delai` = date `"2027-02-02"`.

## Workflow

`EN_ATTENTE_HR` → `EN_ATTENTE_DIRECTION` → `EN_ATTENTE_FINANCE` → `APPROUVEE`, ou `REFUSEE` à n'importe quelle étape.
Chaque étape est décidée par le rôle du même nom (`HR`, `DIRECTION`, `FINANCE`).

## Routes `/api/demandes`

| Méthode | Route | Qui | Réponse |
|---|---|---|---|
| POST | `/` | `MANAGER` | 201 `Demande` |
| POST | `/{id}/valider` | rôle de l'étape | `Demande` (étape suivante) |
| POST | `/{id}/refuser` | rôle de l'étape | `Demande` (`REFUSEE`) |
| GET | `/mes-demandes` | tous | `Demande[]` que j'ai créées |
| GET | `/a-traiter` | tous | `Demande[]` qui attendent ma décision, plus anciennes d'abord |
| GET | `/traitees` | tous | `Demande[]` sur lesquelles j'ai décidé |
| GET | `/` | `ADMIN`, `DIRECTION` | toutes les `Demande[]` |
| GET | `/{id}` | demandeur, décideurs, rôle de l'étape, `ADMIN`, `DIRECTION` | `Demande` |
| GET | `/{id}/validateurs` | mêmes droits que `/{id}` | `[{ date, statut, personnel }]` (validations seulement) |
| GET | `/etapes` | tous | `Etape[]` sans date : création `courante` (moi), validations `a-venir` (progression du formulaire) |

## Créer

```json
{
  "idDepartement": 2,
  "motif": "Nouveau projet web",
  "delai": "2027-02-02",
  "creations": [{ "idFonction": 3, "effectif": 2 }],
  "remplacements": [4]
}
```

- `idDepartement` : le département de **mon** poste.
- `creations` : postes à créer (fonction + effectif ≥ 1, une fonction une seule fois).
- `remplacements` : id de postes existants du département, sans effectif. Interdit pour un poste tenu par un autre
  compte ; autorisé pour le mien.
- Au moins un poste en tout. `motif` obligatoire (200 caractères max). `delai` facultatif, ≥ aujourd'hui + 7 jours.

## `Demande`

```json
{
  "id": 1, "idDepartement": 2, "departement": "Informatique",
  "statut": "EN_ATTENTE_DIRECTION", "statutLibelle": "En attente Direction", "etapeValideur": "DIRECTION",
  "idDemandeur": 1, "demandeur": "manager",
  "date": "2026-11-11T10:23:45", "motif": "Nouveau projet web", "delai": "2027-02-02",
  "creations": [{ "idFonction": 3, "fonction": "Développeur", "effectif": 2 }],
  "remplacements": [{ "idPoste": 4, "poste": "Administrateur système" }],
  "historique": [{ "utilisateur": "hr", "role": "HR", "statut": "EN_ATTENTE_DIRECTION", "date": "2026-11-12T09:02:11" }],
  "aMoiDeDecider": false,
  "suivi": [Etape, …]
}
```

- `etapeValideur` : rôle qui doit décider, `null` si la demande est terminée.
- `aMoiDeDecider` : l'utilisateur connecté peut valider / refuser maintenant.
- `historique` : une ligne par décision, `statut` = statut obtenu.

`Etape` = `{ libelle, etat, texte, date, utilisateur, personnel: Personnel[] }`.
`etat` ∈ `faite`, `courante`, `refusee`, `a-venir`. Le front l'affiche tel quel.
`personnel` : celui qui a agi si l'étape est faite, sinon les comptes du rôle qui décidera.

`Personnel` = `{ idUtilisateur, login, role, roleLibelle, idPoste, poste, detenteur }`.
`detenteur` = « Prénom Nom » de la personne qui occupe le poste (table `emploi`), `null` si vacant.

## Référentiel `/api/referentiel` (tous) → `{ id, label }`

| Route | Contenu |
|---|---|
| `GET /departements` | départements |
| `GET /mon-departement` | un seul objet : département de mon poste |
| `GET /fonctions` | fonctions (pour `creations`) |
| `GET /postes?departement={id}` | postes que je peux faire remplacer (sans ceux des autres comptes, avec le mien) |

## Erreurs → `{ "code", "message" }`

| HTTP | `code` | Cas |
|---|---|---|
| 400 | `VALIDATION` | id inconnu, poste d'un autre département, doublon, effectif < 1, aucun poste, motif vide ou trop long, délai < 7 jours |
| 400 | `REQUETE_ILLISIBLE` | JSON invalide |
| 401 | `NON_CONNECTE` | pas de session |
| 403 | `ACCES_REFUSE` | pas le bon rôle, pas mon département, poste d'un autre compte, demande qui ne me concerne pas, CSRF absent |
| 404 | `RESSOURCE_INTROUVABLE` | demande inconnue |
| 409 | `TRANSITION_INTERDITE` | demande déjà approuvée ou refusée |
