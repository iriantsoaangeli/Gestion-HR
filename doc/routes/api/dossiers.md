# Contrat API — Dossiers de candidature

Connexion obligatoire (session + cookie, voir `doc/user.md`). Tout POST envoie l'en-tête `X-XSRF-TOKEN`.
**HR seulement** (bloc 2 : « Dépôt de dossier : hr ») ; les autres rôles reçoivent 403.
Règles : `doc/conception/regle-metier/bloc2.md` (Précisions : Dossier de candidature).

Dates : `date` = date-heure `"2026-11-11T10:23:45"`, `naissance` = date `"1996-02-11"`.

## Routes `/api/dossiers`

| Méthode | Route | Réponse |
|---|---|---|
| POST | `/` | 201 `Dossier` (multipart, voir ci-dessous) |
| GET | `/` | `Dossier[]` ; `?offre={id}` : ceux de l'offre ; par id croissant |
| GET | `/personnes` | `Personne[]` (candidats possibles), par nom puis prénom |
| GET | `/pieces-jointes/{id}` | le fichier (Content-Type et nom d'origine, `attachment`) |

## Déposer (`multipart/form-data`)

| Champ | |
|---|---|
| `idOffre` | offre **ouverte** |
| `idPersonne` | personne de la base… |
| `nom`, `prenom`, `naissance`, `adresse`, `tel`, `email` | …**ou** nouvelle personne (tous obligatoires), jamais les deux |
| `fichiers` | une part par fichier, au moins un |

- Une personne dépose au plus un dossier par offre.
- Nouvelle personne refusée si elle existe déjà : même e-mail, ou même nom + prénom + naissance (sans casse ni accents).
  `tel` : chiffres, espaces, `+ - . ( )`, 6 à 20 caractères ; `naissance` passée, après 1900.
- Fichiers : 30 Mo au total, aucun vide, pas deux fois le même nom, nom ≤ 255 caractères.
  Chaque fichier devient une `pieceJointe` de la personne, reliée au dossier par `pieceJointeDossier`.
- Si une étape échoue, rien n'est enregistré (ni la personne ni les fichiers).

## `Dossier`

```json
{
  "id": 5, "date": "2026-11-06T16:30:00", "idOffre": 5, "idPersonne": 12,
  "candidat": "Soa Rahelimalala", "email": "soa.rahelimalala@exemple.mg", "tel": "038 55 666 77",
  "employe": false,
  "fichiers": ["CV.pdf", "Lettre.pdf"],
  "piecesJointes": [{ "id": 9, "nom": "CV.pdf", "type": "application/pdf", "taille": 48213 }]
}
```

- `employe` : la personne occupe un poste (table `emploi`). `taille` en octets.
- `fichiers` : noms seuls (affichage) ; `piecesJointes` : les mêmes avec `id` pour le téléchargement.

## `Personne`

```json
{
  "id": 8, "nom": "Rakotoarisoa", "prenom": "Andry", "naissance": "1996-02-11",
  "adresse": "Lot VB 41 Ambatobe, Antananarivo", "tel": "034 11 222 33", "email": "andry.rakotoarisoa@exemple.mg",
  "poste": null, "departement": null
}
```

`poste` / `departement` : poste occupé et son département, `null` pour une personne externe.

## Erreurs → `{ "code", "message" }`

| HTTP | `code` | Cas |
|---|---|---|
| 400 | `VALIDATION` | offre absente, inconnue ou close ; personne absente ou inconnue ; personne de la base et nouvelle à la fois ; champ de nouvelle personne vide, trop long ou invalide ; personne déjà dans la base ; aucun fichier, fichier vide ou en double, plus de 30 Mo |
| 401 | `NON_CONNECTE` | pas de session |
| 403 | `ACCES_REFUSE` | rôle autre que `HR`, CSRF absent |
| 404 | `RESSOURCE_INTROUVABLE` | pièce jointe inconnue |
| 409 | `DEJA_FAIT` | la personne a déjà déposé un dossier pour cette offre |
