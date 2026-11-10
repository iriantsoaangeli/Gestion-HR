# Contrat d'interface : blocs 2 à 4

> Source commune des agents back et front. Ce qui est écrit ici fait foi, et un agent ne s'en écarte pas seul : un écart se signale dans son `<nom>_contexte.md`.
> Tables : `V1__init.sql` (les colonnes et les CHECK font foi). Décisions : [../agents/plan.md](../agents/plan.md).

## 0. Conventions communes

- JSON en camelCase, dates `"2026-12-01"`, horodatages `"2026-12-01T09:30:00"`, montants en nombre (Ar).
- **Erreur** : `{code, message, details}`.
  - 400 si le corps est invalide (`details` donne le message par champ) ;
  - 404 si la ressource n'existe pas ;
  - 422 si une règle métier est violée (`code` dans les tableaux ci-dessous) ;
  - 403 si le rôle est interdit (sécurité active seulement).
- **Transition interdite** : le workflow lève 422 `TRANSITION_INTERDITE`, comme `DemandeWorkflow`.
- **Commentaire** : les actions sans autre donnée prennent `CommentaireRequest {commentaire}` (déjà existant). Il est obligatoire pour un rejet, une annulation ou une rupture, sinon 422 `REFUS_NON_MOTIVE` ou `MOTIF_OBLIGATOIRE`.
- **Historique** : chaque fiche renvoie `historique: [{ancienStatut, nouveauStatut, utilisateurId, dateAction, commentaire}]`, avec en plus `ancienneDate` et `nouvelleDate` pour un entretien.
- **Un contrôleur par ressource, dans le bloc qui la possède**. Une route imbriquée sous la ressource d'un autre bloc va dans le contrôleur de **ton** bloc, avec le chemin complet. Exemple : `POST /api/demandes/{id}/offres` va dans `OffreController`, pas dans `DemandeController`.

## 1. Socle commun : entités, enums et repositories (fait **avant** la vague 1)

Toutes les entités des blocs 2 à 4 sont créées d'un coup, pour que les agents puissent s'appuyer sur celles des autres blocs sans attendre. Les agents de la vague 1 **ne modifient pas** ces classes, sauf pour ajouter une méthode de requête dans un repository de leur bloc.

| Package | Enums | Entités (+ repository chacune) |
|---|---|---|
| `model/recrutement` | `StatutOffre`, `StatutCandidature`, `TypeEntretien`, `StatutEntretien`, `Avis` | `OffreEmploi`, `HistoriqueOffreEmploi`, `Candidat`, `Candidature`, `HistoriqueCandidature`, `Entretien`, `HistoriqueEntretien`, `EntretienParticipant` (`@EmbeddedId`), `EvaluationCandidat` |
| `model/embauche` | `StatutProposition`, `TypeContrat` (partagé avec le bloc 4) | `PropositionEmbauche`, `HistoriquePropositionEmbauche` |
| `model/employe` | `StatutEmploye` (existe déjà) | `Employe` **rendue modifiable** (plus `@Immutable`, `@GeneratedValue`, toutes les colonnes), `HistoriqueEmploye` |
| `model/contrat` | `StatutContrat`, `MotifAffectation` | `Contrat` (`contratPrecedent` en relation), `HistoriqueContrat`, `Avenant`, `Affectation` |
| `model/onboarding` | `StatutOnboarding`, `StatutTache`, `TypeDocument` | `ModeleTacheOnboarding`, `Onboarding`, `OnboardingTache`, `Document` |

Règles : les références à `utilisateur` sont des `Integer`, les autres références sont des relations `@ManyToOne(fetch = LAZY)`, les colonnes d'historique sont en `updatable = false`, et `contextLoads` doit passer.

## 2. Bloc 2 : recrutement (agent back-bloc2)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `GET /api/offres?statut=` | RH, DIRECTION, MANAGER (ses demandes) | → `[OffreResponse]` | |
| `GET /api/offres/{id}` | idem | → `OffreDetailResponse` (offre + candidatures + historique) | |
| `POST /api/demandes/{id}/offres` | RH | `OffreRequest {titre, description, dateCloture}` → `OffreResponse` | `DEMANDE_NON_OUVERTE` |
| `PUT /api/offres/{id}` | RH | `OffreRequest` → `OffreResponse` | `OFFRE_NON_MODIFIABLE` (hors BROUILLON) |
| `POST /api/offres/{id}/publier` | RH | → `OffreResponse` (fixe `datePublication`) | `TRANSITION_INTERDITE` |
| `POST /api/offres/{id}/cloturer`, `/annuler` | RH | `CommentaireRequest` (obligatoire pour annuler) → `OffreResponse`. **Les candidatures non finales passent à REJETEE** | `MOTIF_OBLIGATOIRE` |
| `GET /api/candidats?email=` | RH | → `[CandidatResponse]` | |
| `POST /api/candidats` | RH | `CandidatRequest {nom, prenom, email, telephone, cvChemin, employeId}` → `CandidatResponse` | `EMAIL_DEJA_UTILISE` |
| `GET /api/candidats/{id}` | RH | → `CandidatResponse` | |
| `POST /api/offres/{id}/candidatures` | RH | `CandidatureRequest {candidatId, lettre}` → `CandidatureResponse` | `OFFRE_NON_PUBLIEE`, `OFFRE_CLOTUREE`, `CANDIDATURE_EXISTANTE` |
| `GET /api/offres/{id}/candidatures` | RH, MANAGER (ses demandes) | → `[CandidatureResponse]` | |
| `GET /api/candidatures/{id}` | RH, MANAGER (ses demandes) | → `CandidatureDetailResponse` (candidat, offre, entretiens, évaluations, historique) | |
| `POST /api/candidatures/{id}/statut` | RH | `ChangementStatutRequest {statut, commentaire}` → `CandidatureResponse` | `TRANSITION_INTERDITE`, `REFUS_NON_MOTIVE`, `EVALUATION_MANQUANTE` (vers RETENUE) |
| `POST /api/candidatures/{id}/entretiens` | RH | `EntretienRequest {dateEntretien, type, lieu, participantIds}` → `EntretienResponse` | `CANDIDATURE_NON_ACTIVE`, `PARTICIPANT_INACTIF` |
| `POST /api/entretiens/{id}/reporter` | RH | `ReportRequest {nouvelleDate, commentaire}` → `EntretienResponse` | `ENTRETIEN_NON_PLANIFIE` |
| `POST /api/entretiens/{id}/realiser`, `/annuler` | RH | `CommentaireRequest` → `EntretienResponse` | `TRANSITION_INTERDITE` |
| `GET /api/entretiens/mes-entretiens` | tout utilisateur identifié | → `[EntretienResponse]` (où il est participant) | |
| `POST /api/entretiens/{id}/evaluations` | participant | `EvaluationRequest {critere, note, commentaire, avis}` → `EvaluationResponse` | `ENTRETIEN_NON_REALISE`, `PAS_PARTICIPANT` |

Workflow des candidatures (rôle RH) :
- `RECUE → PRESELECTIONNEE | REJETEE | DESISTEMENT` ;
- `PRESELECTIONNEE → EN_ENTRETIEN | EN_TEST | REJETEE | DESISTEMENT` ;
- `EN_ENTRETIEN ↔ EN_TEST`, puis `→ RETENUE | REJETEE | DESISTEMENT` ;
- `EN_TEST → RETENUE | REJETEE | DESISTEMENT` ;
- RETENUE, REJETEE et DESISTEMENT sont finaux.

Planifier un entretien fait passer une candidature PRESELECTIONNEE à EN_ENTRETIEN (type RH ou TECHNIQUE) ou à EN_TEST (type TEST).

## 3. Bloc 3 : embauche (agent back-bloc3)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `POST /api/candidatures/{id}/propositions` | RH | `PropositionRequest {salaire, dateDebut, typeContrat, dateExpiration}` → `PropositionResponse` | `CANDIDATURE_NON_RETENUE`, `PROPOSITION_EXISTANTE`, `SALAIRE_HORS_BUDGET`, `DATE_EXPIRATION_PASSEE` |
| `GET /api/propositions?statut=` | RH, DIRECTION | → `[PropositionResponse]` | |
| `GET /api/propositions/{id}` | RH, DIRECTION | → `PropositionDetailResponse` (+ candidat, offre, historique) | |
| `POST /api/propositions/{id}/accepter` | RH | → `EmployeDetailResponse` (employé créé, ou existant en mobilité interne) | `PROPOSITION_EXPIREE`, `EMAIL_DEJA_UTILISE`, `TRANSITION_INTERDITE` |
| `POST /api/propositions/{id}/refuser`, `/expirer` | RH | `CommentaireRequest` → `PropositionResponse` | `TRANSITION_INTERDITE` |
| `GET /api/employes` | RH, DIRECTION (+ usage existant du bloc 1) | → `[EmployeResponse]` (format existant inchangé : `id, matricule, nom, prenom, statut`) | |
| `GET /api/employes/{id}` | RH, DIRECTION, l'employé lui-même | → `EmployeDetailResponse` (+ email, téléphone, dateEmbauche, propositionId, historique) | |

L'agent back-bloc3 possède `EmployeController`, `EmployeService` et `EmployeRepository`. Le matricule est `EMP-<année>-<0001>`, tiré d'une séquence `matricule_seq` (V7) ; l'`id` reste la clé primaire.

## 4. Bloc 4 : contrat et onboarding (agent back-bloc4)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `GET /api/employes/{id}/contrats` | RH, l'employé | → `[ContratResponse]` (chaîne incluse) | |
| `POST /api/employes/{id}/contrats` | RH | `ContratRequest {type, dateDebut, dateFin, salaire, propositionId?}` → `ContratResponse` | `CDD_SANS_FIN`, `DATES_INCOHERENTES` |
| `GET /api/contrats/{id}` | RH, l'employé | → `ContratDetailResponse` (+ avenants, historique) | |
| `POST /api/contrats/{id}/activer` | RH | → `ContratResponse` | `CONTRAT_ACTIF_EXISTANT` |
| `POST /api/contrats/{id}/renouveler` | RH | `ContratRequest` → nouveau `ContratResponse` (chaîné, ancien → TERMINE) | `CONTRAT_NON_ACTIF` |
| `POST /api/contrats/{id}/terminer`, `/rompre` | RH | `CommentaireRequest` (motif obligatoire pour rompre) → `ContratResponse` | `MOTIF_OBLIGATOIRE` |
| `POST /api/contrats/{id}/avenants` | RH | `AvenantRequest {dateEffet, objet, nouveauSalaire, motif}` → `AvenantResponse` | `CONTRAT_NON_ACTIF` |
| `GET /api/employes/{id}/affectations`, `GET /api/postes/{id}/affectations` | RH | → `[AffectationResponse]` | |
| `POST /api/employes/{id}/affectations` | RH | `AffectationRequest {posteId, contratId, dateDebut, motif}` → `AffectationResponse` | `POSTE_OCCUPE`, `POSTE_SUPPRIME` |
| `GET /api/modeles-taches`, `POST`, `PUT /api/modeles-taches/{id}` | ADMIN, RH | `ModeleTacheRequest {libelle, roleResponsable, obligatoire, ordre, actif}` → `ModeleTacheResponse` | `LIBELLE_EXISTANT` |
| `POST /api/propositions/{id}/onboarding` | RH | → `OnboardingResponse` (tâches générées) | `ONBOARDING_EXISTANT`, `PROPOSITION_NON_ACCEPTEE` |
| `GET /api/onboardings`, `GET /api/onboardings/{id}` | RH | → `OnboardingResponse {…, avancement: {faites, total}}` | |
| `GET /api/onboarding-taches/a-faire` | tout rôle | → `[TacheResponse]` (pour le rôle de l'utilisateur courant) | |
| `POST /api/onboarding-taches/{id}/faire`, `/annuler` | rôle responsable, ou RH | `CommentaireRequest` → `TacheResponse` | `MAUVAIS_ROLE`, `TACHE_OBLIGATOIRE` |
| `PATCH /api/onboarding-taches/{id}/responsable` | RH | `{responsableId}` → `TacheResponse` | |
| `POST /api/documents` (multipart : `fichier`, `type`, `employeId`, `contratId`, `onboardingTacheId`, `candidatId`) | RH, l'employé | → `DocumentResponse` | `DOCUMENT_SANS_RATTACHEMENT` |
| `GET /api/documents?employeId=&contratId=…`, `GET /api/documents/{id}/fichier` | RH, l'employé | → `[DocumentResponse]` / le fichier | |

Pour le passage du poste en OCCUPE ou VACANT lors d'une affectation, **l'agent n'appelle pas `PosteService`**. Il passe par une interface `ChangementStatutPoste` qu'il déclare dans son bloc, et le coordinateur la branche sur `PosteService` à l'intégration. Les tests utilisent un mock.

## 5. Branchements faits par le coordinateur (vague 3)

1. `DemandeWorkflow` : `RECRUTEMENT_OUVERT → CLOTUREE` (RH, ou automatique quand l'effectif est atteint).
2. Acceptation d'une proposition → contrat BROUILLON (reprise de la proposition) + affectation EMBAUCHE + onboarding EN_COURS, dans la même transaction.
3. Effectif atteint → offre CLOTUREE, demande CLOTUREE, autres candidatures REJETEE motivées.
4. `ChangementStatutPoste` → `PosteService.changerStatut`, et suppression du bouton manuel « Recruter ».
5. Tâche DSI « Créer le compte informatique » → création de l'`utilisateur` SALARIE et renseignement de `employe.utilisateur_id`.

## 6. Contrats sur modèles, fin de recrutement, budget réservé (V11)

- **Types de contrat** : `CDI`, `CDD`, `INTERIM` (« contrat de travail temporaire »), un modèle par type (`doc/conception/contrats/`, `service/ModeleContrat`). `STAGE` retiré.
- **Contrat** : 3 champs en plus dans `ContratRequest` / `ContratResponse` : `periodeEssaiJours`, `heuresHebdo` (1 à 60), `motifRecours`. Un CDD ou un INTERIM ne s'active pas sans motif (422 `MOTIF_RECOURS_OBLIGATOIRE`).
- `GET /api/contrats/{id}/pdf` (RH, DIRECTION) : le contrat rempli sur son modèle, `application/pdf`, `inline`. Employeur : `sirh.employeur.*` dans `application.yml`.
- `POST /api/candidatures/{id}/terminer-recrutement` (RH) : corps `FinRecrutementRequest {typeContrat, dateDebut, dateFin, salaire, periodeEssaiJours, heuresHebdo, motifRecours}` → 201 `{propositionId, employeId, contratId}`. Proposition créée puis acceptée, dossier **sans compte**, contrat BROUILLON complété, affectation, onboarding, clôture si l'effectif est atteint ; une seule transaction. Remplace l'écran « Faire une proposition ».
- **Pas de compte salarié** : plus de création de compte à la tâche DSI ; comptes SALARIE désactivés.
- **Budget des départements** : renvoyé à DIRECTION et TRESORIER, et au MANAGER pour ses départements ; `null` pour les autres (RH, ADMIN…). `DepartementRequest` n'a plus de budget (création à 0) ; `PATCH /api/departements/{id}/budget` `{budget, commentaire}` (DIRECTION, TRESORIER), historisé.
- **Offre** : `OffreRequest.publier` (facultatif) : `true` crée et publie dans la même transaction (historique `null → BROUILLON → PUBLIEE`).

## 7. Recentrage sur le recrutement (V12)

- **Salaires** (proposition, fin de recrutement, contrat, avenant) : entre 200 000 et 3 000 000 Ar, ou 0 avec `motifSalaireNul` (stage, charité). 400 `VALIDATION` (`details.salaireValide`) et 422 `SALAIRE_HORS_LIMITES` / `MOTIF_SALAIRE_NUL_OBLIGATOIRE` (`commun/RegleSalaire`). Le PDF écrit « non rémunéré (motif : …) ».
- **Budget d'une demande** = salaire mensuel prévu par poste ; salaire × effectif ≤ budget du département (`BUDGET_DEPASSE`).
- **1 poste = 1 personne** : à l'ouverture du recrutement, la demande a autant de postes VACANT rattachés (`poste.demande_id`) que son effectif ; chaque fin de recrutement occupe l'un d'eux. `GET /api/postes/demande/{demandeId}`.
- **Choix du candidat** : `GET /api/candidats/choix?departementId=&posteId=&externe=` (RH) → `[{candidatId, employeId, nom, prenom, email, telephone, departement, poste}]`. `POST /api/offres/{id}/candidatures` accepte `employeId` à la place de `candidatId` (mobilité interne).
- **Candidat** : `cvChemin` remplacé par `dossierPapier` ; pièces CV (une seule, 422 `CV_DEJA_DEPOSE`), DIPLOME et CERTIFICATION (plusieurs), autres types refusés (422 `TYPE_DOCUMENT_CANDIDAT`). Fichiers ≤ 10 Mo.
- **Comptes** (Admin) : `POST /api/utilisateurs/{id}/remplacer` `{login, email, motDePasse, depart: QUITTE|CHANGE_DE_ROLE, nouveauRole, motif}` → 201 ; `POST /api/utilisateurs/{id}/desactiver` `{motif}` ; `GET /api/utilisateurs/remplacements`. 422 `REMPLACEMENT_INTERDIT` (Direction, son propre compte), `LOGIN_DEJA_UTILISE`, `EMAIL_DEJA_UTILISE`. `GET /api/utilisateurs?inactifs=true` : annuaire avec les comptes désactivés.
- **Droits** : ADMIN lit tout (offres, candidatures, propositions, dossiers, contrats + PDF, budgets) sans valider, forcer ni fixer de budget. DSI, PAIE, SERVICES_GENERAUX n'ont aucun accès au recrutement (403) et ne peuvent pas participer à un entretien (422 `PARTICIPANT_NON_AUTORISE`).
- Guide des composants retiré du front.


## 8. Corrections du 24/10 (problèmes du 23/10) et navigation

Le cours fait foi : un entretien a un type (RH, technique, test) et reçoit N évaluations ; la proposition vient après une candidature RETENUE. Seuls les problèmes compatibles avec le cours ont été corrigés.

- **Téléphone d'un candidat** : mobile malgache seulement, `032`, `033`, `034`, `037`, `038` ou `039` + 7 chiffres, ou `+261` + 9 chiffres (`commun/RegleTelephone`, 400 `VALIDATION`).
- **Montants** : au plus 2 décimales (`@Digits`) ; salaires ≤ 10 chiffres, budgets ≤ 12 chiffres → 400 sur le champ, au lieu d'un arrondi silencieux ou d'un 409.
- **Entier attendu** (ex. effectif `1.5`) : refusé (`accept-float-as-int: false`), 400 `VALIDATION` avec le champ dans `details`.
- **Fichier > 10 Mo** : 413 `FICHIER_TROP_VOLUMINEUX` au lieu d'un 500.
- **Statut d'une candidature** : `EN_ENTRETIEN` et `EN_TEST` ne se posent plus à la main (422 `STATUT_PAR_PLANIFICATION`) ; ils suivent la planification (entretien RH ou technique → `EN_ENTRETIEN`, test → `EN_TEST`, historisé).
- `GET /api/entretiens/{id}` → `EntretienDetailResponse {entretien, offreId, statutCandidature, evaluations}` (participants et lecteurs de l'offre, sinon 403).
- `CandidatureResponse.prochainEntretien` : date du prochain entretien planifié, dans les listes d'une offre.
- `POST /api/demandes/{id}/arreter-recrutement` (RH) `{commentaire}` obligatoire → offres PUBLIEE → CLOTUREE, BROUILLON → ANNULEE, candidatures en cours REJETEE, demande → CLOTUREE (422 `MOTIF_OBLIGATOIRE`, `RECRUTEMENT_NON_OUVERT`).
- **Poste** : impossible de le supprimer tant qu'une demande en cours le vise (validation ou recrutement ouvert) : 422 `POSTE_EN_RECRUTEMENT`.
- **Front** : menu dans l'ordre du cycle de vie (À faire avec compteurs, Besoin, Recrutement, Embauche et intégration, Référentiel) ; fil d'Ariane (`PageEntete :chemin`) ; fiche d'un entretien `/entretiens/:id` ; boutons de décision explicites et bandeau « Prochaine étape » sur la candidature ; participants limités à RH, managers et Direction.

## 9. Bloc 5 : dossier administratif du salarié (V14)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `GET /api/employes?statut=&departementId=&posteId=&q=` | rôles du recrutement, PAIE | → `[EmployeListeResponse]` (poste, département, contrat, `piecesManquantes`, `dossierComplet`) | |
| `GET /api/employes/{id}` | RH, DIRECTION, ADMIN, PAIE, MANAGER (son équipe) | → `DossierSalarieResponse` (identité, coordonnées masquées selon `droits`, `situation`, `piecesManquantes`, `droits`, `historique`) ; consultation tracée | 403 hors habilitation (tracé) |
| `PUT /api/employes/{id}` | RH | `EmployeModificationRequest` (tous les champs : PUT remplace) → `DossierSalarieResponse` | `DOSSIER_CLOS`, `EMAIL_DEJA_UTILISE`, `CIN_DEJA_UTILISE`, `DATES_INCOHERENTES` ; strict : `CIN_INVALIDE`, `AGE_MINIMUM`, `TELEPHONE_INVALIDE` |
| `POST /api/employes/{id}/suspendre` | RH | `{commentaire}` obligatoire | `MOTIF_OBLIGATOIRE`, `TRANSITION_INTERDITE` |
| `POST /api/employes/{id}/reintegrer` | RH | `{commentaire}` facultatif | `TRANSITION_INTERDITE` |
| `POST /api/employes/{id}/sortir` | RH | `SortieRequest {dateSortie, commentaire}` | `MOTIF_OBLIGATOIRE`, `CONTRAT_ACTIF_EXISTANT`, `DATE_SORTIE_INCOHERENTE`, `TRANSITION_INTERDITE` |
| `GET /api/employes/{id}/carriere` | comme le dossier | → `[EvenementCarriereResponse {date, type, libelle, detail, lien}]` | |
| `GET /api/employes/{id}/modifications` | RH, DIRECTION, ADMIN, PAIE | → `[ModificationEmployeResponse]` | |
| `POST /api/documents` | comme avant | + `dateDelivrance`, `dateExpiration`, `numero` ; une pièce « unique » remplace l'ancienne | `DATE_EXPIRATION_OBLIGATOIRE` (strict), `DATES_INCOHERENTES` |
| `GET /api/documents?employeId=` | comme avant, + PAIE (RIB seulement) | `DocumentResponse` + `dateDelivrance`, `dateExpiration`, `numero`, `remplaceParId`, `etat` | |
| `GET /api/documents/a-renouveler?jours=` | RH, DIRECTION, ADMIN | → `[PieceARenouvelerResponse]` | |
| `GET /api/journal-audit?employeId=&utilisateurId=&action=&du=&au=` | ADMIN, DIRECTION | → `[JournalAuditResponse]` (2 000 plus récentes) | `MAUVAIS_ROLE` |
| `GET /api/parametres` · `PUT /api/parametres/{id}` · `GET /api/parametres/{id}/historique` | tous · RH · tous | `ParametreRequest {valeur, commentaire}` | `PARAMETRE_HORS_BORNES`, `VALEUR_INCHANGEE`, `MAUVAIS_ROLE` |

Contrats (`GET /api/employes/{id}/contrats`, `GET /api/contrats/{id}`) : lecture ouverte à la PAIE, tracée dans le journal. PDF d'un contrat : téléchargement tracé.

## 10. Bloc 6 : temps de travail et pointage (V15)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `GET / POST /api/regimes-travail`, `PUT /api/regimes-travail/{id}` | tous / RH | `RegimeTravailRequest {code, libelle, joursTravailles, heureDebut, heureFin, pauseMinutes, toleranceRetardMinutes, actif}` | `CODE_DEJA_UTILISE`, `HORAIRES_INCOHERENTS`, `REGIME_AU_DELA_DUREE_LEGALE` |
| `GET /api/jours-feries?annee=`, `POST /api/jours-feries` | tous / RH | `{date, libelle}` | `JOUR_FERIE_EXISTANT`, `PERIODE_CLOTUREE` |
| `GET / POST /api/employes/{id}/plannings` | lecture / RH, MANAGER (équipe) | `{regimeId, dateDebut}` | `EMPLOYE_NON_ACTIF`, `REGIME_INACTIF`, `PLANNING_CHEVAUCHE`, `REGIME_INCOMPATIBLE_CONTRAT`, `PERIODE_CLOTUREE` |
| `GET /api/employes/{id}/temps?du=&au=` | RH, DIRECTION, ADMIN, PAIE, MANAGER (équipe) | → `TempsEmployeResponse` | |
| `POST /api/pointages` | RH, MANAGER (équipe) | `{employeId, horodatage, type}` → 201 | `EMPLOYE_NON_ACTIF`, `POINTAGE_FUTUR`, `DATES_INCOHERENTES`, `PERIODE_CLOTUREE`, `PLANNING_ABSENT`, `POINTAGE_EN_DOUBLE` |
| `POST /api/pointages/import` (multipart `fichier`) | RH | CSV `matricule;horodatage;type` → `{lignesLues, importees, rejets[{ligne, contenu, raison}]}` | `FICHIER_VIDE` |
| `POST /api/pointages/{id}/annuler` · `/corriger` | RH, MANAGER (équipe) | `{motif}` · `{horodatage, type, motif}` | `POINTAGE_DEJA_ANNULE`, `MOTIF_OBLIGATOIRE`, `HORODATAGE_OBLIGATOIRE`, + règles de saisie |
| `POST /api/temps/recalculer` | RH, MANAGER (équipe) | `{du, au, employeId?}` → `{journees}` | `DATES_INCOHERENTES` |
| `GET /api/anomalies-pointage?statut=&employeId=&du=&au=` · `/{id}/historique` | périmètre | → `[AnomalieResponse]` | |
| `POST /api/anomalies-pointage/{id}/justifier` · `/ignorer` | RH, MANAGER (équipe) | `{commentaire}` obligatoire | `MOTIF_OBLIGATOIRE`, `TRANSITION_INTERDITE`, `PERIODE_CLOTUREE` |
| `GET /api/heures-sup?statut=&employeId=` · `/{id}` | périmètre | → `[HeureSupResponse]` | |
| `POST /api/heures-sup/{id}/valider` · `/refuser` | RH, MANAGER (équipe) | `{commentaire}` (obligatoire pour refuser) | `REFUS_NON_MOTIVE`, `TRANSITION_INTERDITE`, `PERIODE_CLOTUREE` |
| `GET /api/periodes-temps` · `/{annee}/{mois}` | tous | → `[PeriodeTempsResponse]` (avec ce qui bloque la clôture) | |
| `POST /api/periodes-temps/cloturer` | RH | `{annee, mois, commentaire}` | `MOIS_NON_TERMINE`, `PERIODE_CLOTUREE`, `ANOMALIES_OUVERTES`, `HS_A_VALIDER`, `ABSENCES_A_JUSTIFIER` |
| `GET /api/variables-paie?annee=&mois=` · `/csv` | RH, PAIE, DIRECTION, ADMIN | → `VariablesPaieResponse` · fichier CSV (export tracé) | `MAUVAIS_ROLE` |

« Périmètre » : RH, DIRECTION, ADMIN, PAIE voient tout ; MANAGER voit son équipe.

## 11. Bloc 7 : congés et absences (V16)

| Méthode et route | Rôle | Corps → réponse | Erreurs 422 |
|---|---|---|---|
| `GET / POST /api/types-conge`, `PUT /api/types-conge/{id}` | tous / RH | `TypeCongeRequest` | `CODE_DEJA_UTILISE` |
| `GET /api/employes/{id}/soldes` · `POST …/soldes/recalculer` | périmètre · RH | → `[SoldeCongeResponse]` | |
| `GET / POST /api/employes/{id}/droits` | périmètre / RH | `{typeCongeId, jours, motif}` | `TYPE_SANS_SOLDE`, `JOURS_INVALIDES`, `SOLDE_INSUFFISANT` |
| `POST /api/droits-conge/acquisition` | RH | `{annee, mois}` → `AcquisitionResponse` | `MOIS_NON_TERMINE`, `TYPE_CONGE_INTROUVABLE` |
| `POST /api/demandes-conge` | RH, MANAGER (équipe) | `DemandeCongeRequest {employeId, typeCongeId, dateDebut, dateFin, demiJourneeDebut?, demiJourneeFin?, motif}` → 201 | `EMPLOYE_NON_ACTIF`, `TYPE_CONGE_INACTIF`, `DATES_INCOHERENTES`, `DEMI_JOURNEES_INCOHERENTES`, `DATE_PASSEE`, `HORS_CONTRAT`, `AUCUN_JOUR_DECOMPTE`, `CONGE_CHEVAUCHE`, `SOLDE_INSUFFISANT`, `PLAFOND_ANNUEL_DEPASSE`, `PERIODE_CLOTUREE` |
| `GET /api/demandes-conge?statut=&employeId=` · `/a-valider` · `/{id}` | périmètre | → `[DemandeCongeResponse]` (fiche : `solde`, `historique`) | |
| `GET /api/demandes-conge/simulation?employeId=&typeCongeId=&du=&au=&demiDebut=&demiFin=` | périmètre | → `{nombreJours, decompteSolde, soldeDisponible, soldeApres, problemes[{code, message}]}` | |
| `POST /api/demandes-conge/{id}/valider` | MANAGER (équipe), DIRECTION (sans manager), puis RH | `{commentaire}` facultatif | `TRANSITION_INTERDITE`, `VALIDATION_PAR_LE_MANAGER`, `VALIDATION_PAR_LA_DIRECTION`, `CONGE_CHEVAUCHE`, `PERIODE_CLOTUREE` |
| `POST /api/demandes-conge/{id}/refuser` · `/annuler` | idem · MANAGER, RH | `{commentaire}` obligatoire | `REFUS_NON_MOTIVE`, `MOTIF_OBLIGATOIRE`, `CONGE_COMMENCE` |
| `GET /api/absences?statut=&type=&employeId=` · `/{id}` | périmètre | → `[AbsenceResponse]` (`delaiDepasse`) | |
| `POST /api/absences` | RH, MANAGER (équipe) | `{employeId, type: MALADIE|AUTRE, dateDebut, dateFin, demi…, motif}` → 201 | `TYPE_ABSENCE_INVALIDE`, `ABSENCE_CHEVAUCHE`, `AUCUN_JOUR_DECOMPTE`, `EMPLOYE_NON_ACTIF` |
| `POST /api/absences/{id}/justifier` (multipart `fichier`, `commentaire`) | RH, MANAGER (équipe) | → `AbsenceResponse` | `FICHIER_VIDE`, `TRANSITION_INTERDITE` |
| `POST /api/absences/{id}/non-justifiee` · `/annuler` | RH | `{commentaire}` (obligatoire pour annuler) | `TRANSITION_INTERDITE`, `ABSENCE_DE_CONGE`, `MOTIF_OBLIGATOIRE` |
| `GET /api/calendrier-equipe?departementId=&annee=&mois=` | périmètre | → `CalendrierResponse` | 403 département d'un autre manager |
