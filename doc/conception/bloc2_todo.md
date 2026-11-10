# Todo — Bloc 2 : Recrutement

> Conception : [Conception.md — Bloc 2](Conception.md#bloc-2)
> État : tables dans `V1__init.sql`, **tout le reste à faire**. Prérequis : bloc 1 (demande `RECRUTEMENT_OUVERT`).

Ordre conseillé : offres → candidats / candidatures → entretiens → évaluations → front.

## Base

- [x] Relire les tables existantes : `offre_emploi`, `candidat`, `candidature`, `entretien`, `entretien_participant`, `evaluation_candidat`, `historique_offre_emploi`, `historique_candidature`, `historique_entretien`
- [x] Trancher le doublon CV : `candidat.cv_chemin` ou `document` (`type = 'CV'`) — migration V2 seulement si on change le schéma → **chemin dans `candidat.cv_chemin`** (décision), pas de migration
- [x] Données de test dans `backend/scripts/data_bloc2.sql` (à recopier dans `data.sql` par le coordinateur) : 1 offre par statut, 3-4 candidats (dont 1 interne avec `employe_id`), candidatures à plusieurs statuts, 2 entretiens, quelques évaluations
- [x] Mettre à jour `dataReset.sql` si de nouvelles tables apparaissent → rien à faire (aucune table nouvelle, le script vide toutes les tables)

## Modèle / Repository

- [x] Enums : `StatutOffre` (`BROUILLON`, `PUBLIEE`, `CLOTUREE`, `ANNULEE`), `StatutCandidature` (`RECUE`, `PRESELECTIONNEE`, `EN_ENTRETIEN`, `EN_TEST`, `RETENUE`, `REJETEE`, `DESISTEMENT`), `TypeEntretien` (`RH`, `TECHNIQUE`, `TEST`), `StatutEntretien` (`PLANIFIE`, `REALISE`, `ANNULE`), `Avis` (`FAVORABLE`, `RESERVE`, `DEFAVORABLE`)
- [x] `OffreEmploi` + `HistoriqueOffreEmploi` + repositories (package `model/recrutement`)
- [x] `Candidat` (`employe_id` en `Integer` ou relation vers `Employe`) + repository (`findByEmail`)
- [x] `Candidature` + `HistoriqueCandidature` + repositories (`findByOffreId…`, `existsByCandidatIdAndOffreId`)
- [x] `Entretien` + `HistoriqueEntretien` (avec `ancienne_date` / `nouvelle_date`) + repositories
- [x] `EntretienParticipant` (clé composée `@EmbeddedId`) + repository
- [x] `EvaluationCandidat` + repository
- [x] Colonnes d'historique en `updatable = false`
- [x] `contextLoads` passe avec les nouvelles entités

## Services & règles

- [x] `OffreService.creer` : seulement depuis une demande `RECRUTEMENT_OUVERT` (`DEMANDE_NON_OUVERTE`), `null → BROUILLON` historisé
- [x] `OffreService.modifier` (en `BROUILLON` seulement), `publier` (fixe `date_publication`), `cloturer`, `annuler` (motivé) + `historique_offre_emploi`
- [x] `CandidatService` : créer (email unique → 422 `EMAIL_DEJA_UTILISE`), retrouver par email, lier un employé pour une candidature interne
- [x] `CandidatureWorkflow` (statut × rôle → suivants), sur le modèle de `DemandeWorkflow`
- [x] `CandidatureService.postuler` : offre `PUBLIEE` (`OFFRE_NON_PUBLIEE`), pas après `date_cloture` (`OFFRE_CLOTUREE`), une seule fois par offre (`CANDIDATURE_EXISTANTE`), `null → RECUE` historisé
- [x] `CandidatureService.changerStatut` : via le workflow, `REJETEE` motivé (`REFUS_NON_MOTIVE`), ligne `historique_candidature`
- [x] `EntretienService.planifier` : candidature au bon statut, participants actifs, passe la candidature en `EN_ENTRETIEN` si besoin
- [x] `EntretienService.reporter` : statut `PLANIFIE` inchangé, ligne `historique_entretien` avec ancienne / nouvelle date
- [x] `EntretienService.realiser` / `annuler` + historique
- [x] `EvaluationService.evaluer` : entretien `REALISE`, évaluateur participant, note 0-20, évaluation jamais modifiée
- [x] Règle « `RETENUE` seulement avec au moins une évaluation » → 422 `EVALUATION_MANQUANTE` (décision)
- [x] Trancher : effet de l'annulation / clôture d'une offre sur les candidatures en cours → REJETEE, motif « Offre clôturée » / « Offre annulée » (décision)

## API

- [x] DTO `OffreRequest` / `OffreResponse`, `CandidatRequest` / `CandidatResponse`, `CandidatureRequest` / `CandidatureResponse` / `CandidatureDetailResponse`, `EntretienRequest` / `EntretienResponse`, `EvaluationRequest` / `EvaluationResponse`, `ChangementStatutRequest`
- [x] `GET /api/offres`, `GET /api/offres/{id}`, `POST /api/demandes/{id}/offres`, `PUT /api/offres/{id}`
- [x] `POST /api/offres/{id}/publier`, `/cloturer`, `/annuler`
- [x] `GET /api/candidats?email=`, `POST /api/candidats`, `GET /api/candidats/{id}`
- [x] `POST /api/offres/{id}/candidatures`, `GET /api/offres/{id}/candidatures`, `GET /api/candidatures/{id}`, `POST /api/candidatures/{id}/statut`
- [x] `POST /api/candidatures/{id}/entretiens`, `POST /api/entretiens/{id}/reporter`, `/realiser`, `/annuler`, `GET /api/entretiens/mes-entretiens`
- [x] `POST /api/entretiens/{id}/evaluations`

## Sécurité / droits

- [x] `@PreAuthorize` : offres, candidats, candidatures, planification → `RH`
- [x] Évaluations : participant de l'entretien (contrôle dans le service)
- [x] `MANAGER` : lecture des candidatures des offres issues de ses demandes seulement
- [x] Auteur (`cree_par`, `evaluateur_id`, `utilisateur_id`) toujours via `UtilisateurCourant`

## Tests

- [x] `CandidatureWorkflowTest` : transitions autorisées / interdites, statuts finaux
- [x] `OffreServiceTest` : création refusée hors `RECRUTEMENT_OUVERT`, publication sans date, historique écrit
- [x] `CandidatureServiceTest` : double candidature refusée, offre clôturée, rejet sans commentaire refusé
- [x] `EntretienServiceTest` : report historisé avec les dates, participant inactif refusé
- [x] `EvaluationServiceTest` : évaluation sur entretien non réalisé refusée, évaluateur non participant refusé

## Front Vue.js

- [ ] RH : depuis une demande `RECRUTEMENT_OUVERT`, créer / modifier / publier une offre
- [ ] Liste des offres (filtre statut), fiche offre avec ses candidatures
- [ ] Saisie d'un candidat (recherche par email d'abord) et de sa candidature
- [ ] Pipeline des candidatures d'une offre (colonnes par statut), actions de changement de statut, motif au rejet
- [ ] Planification d'un entretien (date, type, lieu, participants), report, réalisation, annulation
- [ ] « Mes entretiens » (participant) + formulaire d'évaluation
- [ ] Fiche candidature : entretiens, évaluations, historique

## Démo

- [x] Scénario : `rh1` crée et publie une offre depuis la demande ouverte → 3 candidatures → présélection → entretien avec `manager1` → évaluations → 1 `RETENUE`, 1 `REJETEE` motivée, 1 `DESISTEMENT`
- [x] Contre-tests : double candidature, candidature sur offre clôturée, rejet sans motif, évaluation par un non-participant
- [x] Vérifier en base : lignes `historique_offre_emploi`, `historique_candidature`, `historique_entretien`
