# Contrat API — Entretiens et évaluations

Connexion obligatoire (session + cookie, voir `doc/user.md`). Tout POST envoie l'en-tête `X-XSRF-TOKEN`.
L'évaluateur n'est jamais dans le JSON : c'est le poste du compte connecté (`utilisateur.idPoste`).
Règles : `doc/conception/regle-metier/bloc2.md` (Précisions : Entretien, Évaluation).

Dates : date-heure locale `"2026-11-18T09:00:00"` (pas d'UTC, pas de fuseau).

Accès : `MANAGER`, `HR`, `DIRECTION` (tous les rôles sauf `FINANCE` et `ADMIN`) ; les autres reçoivent 403.

## Statut

`EN_ATTENTE` (« En attente ») à la préparation → `FINI` (« Fini ») quand tous les interviewers ont évalué
(mis à jour par le backend). Table `statusEntretien`, enum `StatutEntretien`.

## Routes `/api/entretiens`

| Méthode | Route | Qui | Réponse |
|---|---|---|---|
| POST | `/` | `HR` | 201 `Entretien` (`EN_ATTENTE`) |
| POST | `/{id}/evaluation` | interviewer de l'entretien, une fois | 201 `Entretien` (avec `idEvaluation`, `FINI` si c'était la dernière) |
| GET | `/en-attente` | accès | `Entretien[]`, plus proches d'abord |
| GET | `/finis` | accès | `Entretien[]`, plus récents d'abord |
| GET | `/?offre={id}` | accès | `Entretien[]` de l'offre (404 si offre inconnue) ; sans `offre` : tous |
| GET | `/{id}` | accès | `Entretien` |
| GET | `/interviewers-possibles` | `HR` | `Interviewer[]` : postes des comptes actifs `MANAGER`, `HR`, `DIRECTION` |

## Routes `/api/evaluations` (mes évaluations)

| Méthode | Route | Qui | Réponse |
|---|---|---|---|
| GET | `/a-faire` | accès | `Entretien[]` dont je suis interviewer et que je n'ai pas évalués |
| GET | `/faites` | accès | `Entretien[]` que j'ai évalués (`maNote`, `idEvaluation`) |
| GET | `/{id}` | accès, sauf si les notes de l'entretien me sont cachées (403) | `FicheEvaluation` |

## Préparer (HR)

```json
{ "idDossier": 4, "date": "2026-11-20T10:00:00", "interviewers": [6, 7] }
```

- `idDossier` : un dossier existant d'une offre **ouverte**. Un candidat peut avoir plusieurs entretiens.
- `date` : dans le futur.
- `interviewers` : au moins un `idPoste`, tous dans `/interviewers-possibles`, sans doublon.

## Évaluer (interviewer connecté)

```json
{ "note": 75, "remarques": "Bon contact, connaît bien Linux." }
```

- `note` : entier de 0 à 100 (`50.5` refusé). `remarques` : obligatoires, 5000 caractères max.
- Une seule évaluation par interviewer. Pas avant la date de l'entretien ; possible même si l'offre est close depuis.

## `Entretien`

```json
{
  "id": 4, "date": "2026-11-10T08:30:00", "idDossier": 5, "idOffre": 2,
  "poste": "Développeur", "idDepartement": 2, "departement": "Informatique",
  "idPersonne": 14, "candidat": "Haja Andriamanana",
  "statut": "EN_ATTENTE", "statutLibelle": "En attente", "fini": false,
  "interviewers": [Interviewer, …],
  "evaluations": [Evaluation, …],
  "note": 70, "notesCachees": false,
  "monPoste": 7, "peutEvaluer": false, "raisonEvaluer": "Vous avez déjà évalué cet entretien.",
  "maNote": 70, "idEvaluation": 1
}
```

- `poste` : intitulé de l'offre ; `departement` : celui du premier poste de l'offre, à défaut du poste responsable (comme la liste des offres). Offre et candidat sont lus par le dossier.
- `fini` : `statut` vaut `FINI` (le front teste ce booléen, jamais la valeur).
- `note` : moyenne arrondie des évaluations, `null` sans évaluation.
- **Notes séparées** : si je suis interviewer et que je n'ai pas encore évalué, `notesCachees: true`, `note: null`
  et chaque évaluation a `note: null`, `remarques: null` (je vois seulement qui a évalué). Les autres voient tout.
- Pour l'utilisateur connecté : `monPoste` = mon `idPoste` si je suis interviewer (sinon `null`) ;
  `peutEvaluer` ; `raisonEvaluer` = `"Vous n'êtes pas interviewer de cet entretien."`,
  `"Vous avez déjà évalué cet entretien."`,
  `"L'entretien n'a pas encore eu lieu : vous pourrez l'évaluer après sa date."` ou `null` (dans cet ordre) ; `maNote` / `idEvaluation` = mon évaluation ou `null`.

`Interviewer` = `{ idPoste, poste, idDepartement, departement, detenteur, login }`
(`detenteur` = « Prénom Nom » du détenteur du poste, `null` si vacant).

`Evaluation` = `{ id, idPoste, poste, departement, detenteur, login, date, note, remarques }`.

`FicheEvaluation` = `Evaluation` + `{ deMoi, entretien: Entretien }` (`deMoi` : je l'ai écrite).

## Erreurs → `{ "code", "message" }`

| HTTP | `code` | Cas |
|---|---|---|
| 400 | `VALIDATION` | dossier absent ou inconnu, offre close, date absente ou passée, aucun interviewer, interviewer non autorisé ou en double, note absente / décimale / hors 0-100, remarques vides ou trop longues |
| 400 | `REQUETE_ILLISIBLE` | JSON invalide, date mal écrite, texte à la place d'un nombre |
| 401 | `NON_CONNECTE` | pas de session |
| 403 | `ACCES_REFUSE` | `FINANCE` / `ADMIN`, préparer sans être `HR`, évaluer sans être interviewer, fiche d'évaluation avant d'avoir rendu la mienne, CSRF absent |
| 404 | `RESSOURCE_INTROUVABLE` | entretien, évaluation ou offre inconnus |
| 409 | `DEJA_FAIT` | j'ai déjà évalué cet entretien |
| 409 | `ENTRETIEN_A_VENIR` | évaluer avant la date de l'entretien |
