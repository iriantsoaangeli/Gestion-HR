# Revue du code — bloc 2 (offres, dossiers, entretiens)

Revue du 2026-11-13 : relecture « nouveau développeur » + tests (Java et `script/test/`) sur la base d'essai.
Chaque problème : **où**, **ce qui gêne**, **correction proposée**. Rien n'est corrigé tant que ce n'est pas validé.

Chemins : `B/` = `backend/src/main/java/gestion/hr/`, `F/` = `frontend/src/`.

---

## A. Gênant (à corriger avant la fin du bloc 2)

### A1. Trois façons de coder un statut
| Fonctionnalité | Base | Java |
|---|---|---|
| Demande | table `status` + id | enum `Statut` |
| Entretien | table `statusEntretien` + id | enum `StatutEntretien` + `StatutEntretienConverter` |
| Offre | colonne texte + `CHECK` | enum `StatutOffre` **et** constantes `Offre.OUVERT` / `Offre.CLOS` (`B/offre/Model/Offre.java:20-21`) |

- Gêne : on ne sait pas quel modèle suivre pour le prochain statut ; l'offre a deux sources pour les mêmes valeurs.
- Correction : garder seulement l'enum `StatutOffre` dans `Offre` (supprimer les constantes texte). Choisir un modèle unique pour les prochains statuts.

### A2. « Déjà fait » : trois réponses différentes
| Cas | Réponse | Où |
|---|---|---|
| Ligne de demande déjà publiée | 409 `DEJA_PUBLIEE` | `B/offre/Service/PublicationOffre.java:153` |
| Entretien déjà évalué | 409 `TRANSITION_INTERDITE` | `B/entretien/Service/RedactionEvaluation.java:54` |
| Dossier déjà déposé | 400 `VALIDATION` | `B/dossier/Service/DepotDossier.java:125` |

- Gêne : le front ne peut pas traiter ces cas de la même façon ; `TRANSITION_INTERDITE` ne dit pas « déjà évalué ».
- Correction : un seul code pour un doublon, ex. 409 `DEJA_FAIT` (contrats `doc/routes/api/*.md` à mettre à jour).

### A3. Constructeurs à 15-20 arguments sans nom
- `B/entretien/Service/ConversionEntretien.java:87-93` : ~20 arguments ; `raison == null` y est passé pour `peutEvaluer`.
- `B/offre/Service/ConversionOffre.java:53-58` (16 arguments), `B/entretien/Service/LectureEntretien.java:100`.
- Gêne : impossible de savoir quelle valeur va dans quel champ sans ouvrir le record.
- Correction : une variable nommée par valeur calculée avant l'appel (`boolean peutEvaluer = raison == null;`).

### A4. Le département d'une offre est calculé deux fois, différemment
- `B/offre/Service/ConversionOffre.java:47` : département du premier poste couvert.
- `B/entretien/Service/ConversionEntretien.java` (`departementDe`) : premier poste couvert, **sinon** poste responsable.
- Gêne : la liste des offres et la fiche d'un entretien peuvent afficher deux départements différents.
- Correction : une seule méthode (côté offre), utilisée par les deux.

### A5. Le contrôle « HR seulement » écrit 4 fois sous 4 noms
- `B/offre/Service/PublicationOffre.java:84` (en ligne), `B/offre/Service/LectureOffre.java:86` (`exigerHr`),
  `B/dossier/Service/AccesDossier.java` (`verifierHr`), `B/entretien/Service/AccesEntretien.java:48` (`hr()`).
- Correction : une classe `AccesOffre` comme pour dossier et entretien, et le même nom de méthode partout.

### A6. Statuts écrits en dur dans le front
- `'OUVERT'` / `'CLOS'` : `F/views/offre/OffresView.vue:27-28`, `F/api/dossiers.js:8`, `F/components/offre/FicheOffre.vue:21`.
- `'FINI'` : `F/components/offre/FicheOffre.vue:47`, `F/components/entretien/FicheEntretien.vue:38`,
  `F/components/entretien/EvaluationForm.vue:54`, `F/components/entretien/FicheEvaluation.vue:87`.
- Gêne : contraire à la norme « rien de métier en dur dans les vues ».
- Correction : un booléen dans la réponse (`ouverte`, `fini`), ou un utilitaire unique dans `F/utils/`.

### A7. Création de l'offre recopiée
- `B/offre/Service/PublicationOffre.java:109` et `:120` : deux `new Offre(...)` identiques à 7 arguments (branches création / remplacement).
- Gêne : ajouter un champ à l'offre = penser à modifier les deux.
- Correction : créer l'offre une fois, avant la branche.

### A8. Code mort
- `Offre.setStatus` (`B/offre/Model/Offre.java:79`) et `Offre.CLOS` : rien ne ferme une offre aujourd'hui.
- Correction : retirer, ou commenter « clôture : à faire (règle « l'offre est arrêtée » de bloc2.md) ».

---

## B. Détails

| # | Où | Gêne | Correction |
|---|---|---|---|
| B1 | `PublicationOffre` (`obligatoire`, `facultatif`, `borne`), `DepotDossier` (`texte`, `vide`), `RedactionEvaluation` (en ligne) | même contrôle de texte écrit 3 fois | un utilitaire dans `B/commun/` |
| B2 | `dateCourte` dans `F/components/dossier/DossierForm.vue`, `F/components/entretien/PreparationEntretienForm.vue`, `F/components/offre/FicheOffre.vue` | même fonction, 3 variantes | la mettre dans `F/utils/dates.js` |
| B3 | `PAS_INTERVIEWER`, `DEJA_EVALUE` dans `ConversionEntretien` | messages de droit utilisés par `RedactionEvaluation` | les déplacer dans `AccesEntretien` |
| B4 | `LectureEntretien.trouver(entretiens, id)` statique | reçoit le repository en paramètre | méthode d'instance |
| B5 | `OffreForm.vue` (`publier`), `DossierForm.vue` (`deposer`) | ternaires imbriqués sur 4-5 niveaux pour les erreurs | une suite de `if` |
| B6 | front `dateLimite` / API `delai` ; offre `status` / entretien `statut` | une notion, deux noms | un seul nom par notion |
| B7 | `README.md:204` | ligne `k` oubliée | supprimer |

---

## C. La doc ne dit pas la même chose que le code

| # | Doc | Écart |
|---|---|---|
| C1 | `doc/conception/regle-metier/bloc2.md`, section Workflow | « Entretien : manager » alors que les Précisions et le code disent « HR prépare » ; « Évaluation : manager et hr » alors que DIRECTION peut aussi évaluer (`AccesEntretien.ROLES`) |
| C2 | `bloc2.md`, Entretien / Recrutement | « le manager a la décision finale » et « l'offre est arrêtée » : pas encore codés, la doc ne le signale pas |
| C3 | `doc/conception/base/schema.md:6` | `#idDepartment` au lieu de `#idDepartement` |
| C4 | `schema.md:82` | `status:ouvert/clos` en minuscules, la base a `'OUVERT'` / `'CLOS'` |
| C5 | `README.md` | ligne 4 « Bloc 2 en cours : offres d'emploi » sans dossiers ni entretiens ; l'arbre cite `qol.md` au lieu du dossier `qol/` |

---

## D. Trouvé par les tests et déjà corrigé

| Problème | Correction |
|---|---|
| Le JSON acceptait `"50"` comme note, arrondissait `1.5` à `1` (effectif, id), prenait `""` pour une date vide et une date-heure pour une date (bloc 1 et bloc 2) | JSON strict dans `backend/src/main/resources/application.properties` |
| Envoi de plus de 31 Mo : erreur sans `{ code, message }` | `GestionErreurs` : 400 `VALIDATION` « dépassent 30 Mo » |
| 3 tests de `DossierControllerTest` faux : `req.param()` ajoutait une 2e valeur au lieu de remplacer | `nouvelle(req, champ, valeur…)` remplace |
| Script dossier : un e-mail commençant par `@` était lu par curl comme un fichier | `--form-string` pour les champs texte |
| Front : « 1 Ko sur 30 Mo » sans fichier, cadre « Fichiers non ajoutés » toujours affiché | `DossierForm.vue` |

**Reste en échec** (futur, voir `doc/qol/qol1.md`) : `script/test/demande` — délai de l'an 9999 et effectif de 2 milliards acceptés.

---

## E. Ce qui aide à comprendre le code (à garder)

- `README.md` : arborescence, normes, ordre règle → schéma → API → code.
- Les Javadoc de `OffreService`, `DossierService`, `EntretienService` : la liste des classes du package et leur rôle.
- Les en-têtes de classe qui reprennent la phrase de `bloc2.md` : un `grep` sur la règle tombe sur le code
  (`DepotDossier` pour « un dossier par offre », `ConversionEntretien` pour les notes séparées, `PublicationOffre` pour les postes vides).
- Les en-têtes des composants Vue : rôle, route appelée, fichier de doc.

## F. Ce qui manque pour démarrer

- Une page « parcours recrutement » : offre → dossier → entretien → évaluations → décision, qui fait quoi, ce qui n'existe pas encore.
- L'explication « un interviewer est un **poste**, pas un compte » (`idPoste`, `monPoste`).
- La liste des codes d'erreur communs (`VALIDATION`, `ACCES_REFUSE`…) et leur sens.
- Une checklist « ajouter un champ » (ex. offre : schema.md, init.sql, demo.sql, offres.md → `Offre`, `PublierOffreRequest`,
  `PublicationOffre`, `OffreResponse`, `ConversionOffre` → `OffreForm.vue`, `FicheOffre.vue` → `OffreControllerTest`, script de test).

---

## G. Test dans le navigateur (parcours complet, comptes hr / manager / direction / finance)

Le parcours entier marche : offre → dossier (personne existante et nouvelle) → entretien → 3 évaluations,
notes cachées tant qu'on n'a pas évalué, pièces jointes téléchargeables. Captures : `~/agent-nav-shots/`.

### G1. Gênant

| # | Page | Problème | Correction proposée |
|---|---|---|---|
| G1.1 | `/offres/nouvelle` | une demande dont toutes les lignes ont déjà une offre reste dans le tableau ; « Publier » ne montre que la bulle du navigateur « Please select an item » | cacher (ou marquer « tout publié ») ces demandes ; message clair si plus rien à publier |
| G1.2 | `/entretiens/:id/evaluation` | on peut évaluer avant la date de l'entretien, qui passe « Fini » avant d'avoir eu lieu | **à décider** : le contrat dit aujourd'hui « pas de contrôle de date » |
| G1.3 | toutes (390 px, téléphone) | le menu reste affiché et le contenu est écrasé et rogné | menu repliable sous ~800 px |
| G1.4 | `/entretiens/:id/evaluation` | bouton « Enregistrer » grisé sans dire que les remarques sont obligatoires | message sous le champ, comme pour la note |
| G1.5 | `/dossiers/nouveau`, `/entretiens/nouveau` (1366 px) | textes coupés (« Dévelop… », « Departemen ») sans info-bulle ; deux offres « Développeur… » impossibles à distinguer | `title` sur les cellules coupées ; moins de colonnes |

### G2. Détails

| # | Page | Problème |
|---|---|---|
| G2.1 | `/dossiers/nouveau`, `/entretiens/nouveau` | un message d'erreur reste affiché après correction (« Choisissez une offre. ») ; l'ancien message vert reste à côté d'une nouvelle erreur rouge |
| G2.2 | `/dossiers/nouveau` (nouvelle personne) | les erreurs arrivent une par une (naissance, puis adresse, puis téléphone…) |
| G2.3 | `/offres/nouvelle` | message de succès peu visible (sous le formulaire, sans défilement) ; codes `INF-2026-0005` non expliqués |
| G2.4 | plusieurs | le manager s'appelle « Hery Rakotomalala » à la création d'offre, « Manager informatique » ailleurs ; dates en 3 formats (2026-12-03, 03/12/2026, 20 décembre 2026) |
| G2.5 | `/offres/:id` | pas de bouton « Déposer un dossier » sur la fiche d'une offre ouverte |
| G2.6 | `/entretiens/:id` (manager, direction) | « Voir l'offre (non accessible) » affiché ; après évaluation, bouton « Évaluer » grisé au lieu d'un lien vers mon évaluation |
| G2.7 | `/entretiens/:id` (finance) | renvoyé vers « Demandes à traiter » sans explication |

### G3. Ce qui a manqué au testeur

- Le lien compte ↔ personne ↔ poste (hr = Lalao Rasoanaivo, Responsable RH…) : absent de `doc/user.md`.
- Savoir que toutes les demandes approuvées de la démo ont déjà leur offre (il a dû créer et faire valider une demande).
