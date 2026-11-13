# Contrat API — Offres d'emploi

Connexion obligatoire (session + cookie, voir `doc/user.md`). Tout POST envoie l'en-tête `X-XSRF-TOKEN`.
L'émetteur n'est jamais dans le JSON : c'est l'utilisateur connecté. Règles : `doc/conception/regle-metier/bloc2.md`.

Dates : `date` = date-heure `"2026-11-11T10:23:45"`, `delai` = date `"2027-02-02"`.

## Routes `/api/offres`

| Méthode | Route | Qui | Réponse |
|---|---|---|---|
| GET | `/` | `HR` | `Offre[]`, plus récentes d'abord |
| GET | `/{id}` | `HR` | `Offre` |
| GET | `/demandes-approuvees` | `HR` | `DemandeApprouvee[]` (formulaire de création), plus récentes d'abord |
| POST | `/` | `HR` | 201 `Offre` |

## Publier

Une offre = une demande approuvée et **une ligne** de cette demande : une création (`idFonction`) **ou** un
remplacement (`idPoste`), jamais les deux.

```json
{
  "idDemande": 10,
  "idFonction": 5,
  "idPoste": null,
  "intitule": "Comptable",
  "description": "Comptable confirmé(e) pour la clôture annuelle…",
  "profil": "Bac+3 en comptabilité",
  "delai": "2026-12-15"
}
```

- `intitule` obligatoire (150 caractères max), `description` obligatoire (4000 max), `profil` facultatif (4000 max).
- `delai` : date limite de candidature, facultative, au moins demain.
- Création : le backend crée `effectif` postes vides dans le département de la demande (label = la fonction),
  avec `fonctionPoste`, `emploi` vacant et `creationPoste`, code `<code du département>-<année>-<compteur>`
  (ex. `INF-2026-0007`, compteur sur 4 chiffres par département et par année, à la suite du plus grand existant).
- Remplacement : l'offre couvre le poste existant.
- `idPosteResponsable` = poste du manager qui a fait la demande ; `idEmetteur` = moi. L'offre démarre `OUVERT`.
- Une ligne déjà couverte par une offre (ouverte ou close) ne peut pas être republiée.

## `Offre`

```json
{
  "id": 4, "intitule": "Comptable",
  "idDepartement": 3, "departement": "Finance",
  "idPosteResponsable": 12, "responsable": "Manager finance",
  "description": "…", "profil": null,
  "statut": "OUVERT", "statutLibelle": "Ouverte", "ouverte": true, "delai": "2026-12-15",
  "idDemande": 10, "idEmetteur": 2, "emetteur": "hr",
  "postes": [{ "id": 15, "code": "FIN-2026-0006", "label": "Comptable" }],
  "candidats": 0
}
```

- `statut` ∈ `OUVERT` (reçoit des dossiers), `CLOS` (colonne `offre.status`). `statutLibelle` : texte à afficher.
  `ouverte` : `statut` vaut `OUVERT` (le front teste ce booléen, jamais la valeur).
- `departement` : celui du premier poste couvert, à défaut du poste responsable ; `responsable` : libellé du poste responsable.
- `postes` : postes couverts (`offrePoste`), avec leur code. `candidats` : nombre de dossiers déposés.

## `DemandeApprouvee`

```json
{
  "id": 10, "date": "2026-10-03T00:00:00", "delai": "2026-11-02",
  "idDepartement": 3, "departement": "Finance",
  "demandeur": { "login": "manager.finance", "detenteur": "Fidy Randrianasolo" },
  "motif": "Clôture annuelle : renfort comptable",
  "creations": [{ "idFonction": 5, "fonction": "Comptable", "effectif": 2, "idOffre": 1 }],
  "remplacements": [{ "idPoste": 3, "poste": "Développeur", "code": "INF-2026-0001", "idOffre": null }]
}
```

`idOffre` : offre qui couvre déjà la ligne, `null` si la ligne peut encore être publiée.

## Erreurs → `{ "code", "message" }`

| HTTP | `code` | Cas |
|---|---|---|
| 400 | `VALIDATION` | demande inconnue ou pas approuvée, ligne absente de la demande, création et remplacement à la fois (ou aucun), intitulé / description vides ou trop longs, date limite avant demain |
| 400 | `REQUETE_ILLISIBLE` | JSON invalide |
| 401 | `NON_CONNECTE` | pas de session |
| 403 | `ACCES_REFUSE` | rôle autre que `HR`, CSRF absent |
| 404 | `RESSOURCE_INTROUVABLE` | offre inconnue |
| 409 | `DEJA_FAIT` | la ligne de la demande a déjà une offre |
