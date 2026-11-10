# Todo — Bloc 4 : Contrat & Onboarding

> Conception : [Conception.md — Bloc 4](Conception.md#bloc-4)
> État : tables dans `V1__init.sql`, **tout le reste à faire**. Prérequis : bloc 3 (employé créé, proposition `ACCEPTEE`).

Ordre conseillé : contrats → avenants → affectations (poste `OCCUPE`) → modèle de checklist → onboarding et tâches → documents → front.

## Base

- [x] Relire `contrat`, `avenant`, `affectation`, `modele_tache_onboarding`, `onboarding`, `onboarding_tache`, `document`, `historique_contrat`
- [x] Données de test : les 6 tâches du cours dans `modele_tache_onboarding` (RH, SERVICES_GENERAUX, DSI ×2, MANAGER, PAIE) ; un utilisateur par rôle `DSI`, `PAIE`, `SERVICES_GENERAUX` ; un employé avec CDD → renouvellement → CDI (exemple du cours)
- [x] Choisir le stockage GED (dossier local configurable au MVP) et le format de `chemin_ged`

## Modèle / Repository

- [x] Enums `StatutContrat` (`BROUILLON`, `ACTIF`, `TERMINE`, `ROMPU`), `MotifAffectation` (`EMBAUCHE`, `MUTATION`, `PROMOTION`), `StatutOnboarding` (`EN_COURS`, `TERMINE`), `StatutTache` (`A_FAIRE`, `FAIT`, `ANNULEE`), `TypeDocument` (`CV`, `LETTRE_MOTIVATION`, `CONTRAT_SIGNE`, `PIECE_IDENTITE`, `RIB`, `DIPLOME`, `AUTRE`)
- [x] `Contrat` (avec `contrat_precedent_id`) + `HistoriqueContrat` + repositories (package `model/contrat`)
- [x] `Avenant` + repository
- [x] `Affectation` + repository (`findByEmployeIdAndDateFinIsNull`, `findByPosteId…`)
- [x] `ModeleTacheOnboarding`, `Onboarding`, `OnboardingTache` + repositories (package `model/onboarding`)
- [x] `Document` + repository
- [x] `contextLoads` passe avec les nouvelles entités

## Services & règles

- [x] `ContratWorkflow` : `BROUILLON → ACTIF → TERMINE / ROMPU`, rôle `RH`
- [x] `ContratService.creer` : référence générée unique, reprise de la proposition pour le premier contrat, règles CDD / CDI / dates ; `null → BROUILLON` historisé
- [x] `ContratService.activer` : un seul contrat `ACTIF` par employé (`CONTRAT_ACTIF_EXISTANT`) ; contrat signé exigé (à trancher)
- [x] `ContratService.renouveler` : nouveau contrat chaîné (`contrat_precedent_id`), ancien `→ TERMINE`
- [x] `ContratService.terminer` / `rompre` (`motif_fin` obligatoire) + `historique_contrat`
- [x] `AvenantService.creer` : contrat `ACTIF`, `ancien_salaire` = salaire courant, `valide_par` = utilisateur courant ; trancher le calcul du salaire courant
- [x] `AffectationService.affecter` : poste `VACANT` → `OCCUPE` (via `ChangementStatutPoste`, à brancher sur `PosteService`, ligne `historique_poste`) ; ferme l'affectation ouverte précédente ; ancien poste → `VACANT`
- [ ] Remplacer le passage manuel du poste en `OCCUPE` (bloc 1) par l'affectation
- [x] `OnboardingService.creer` : un par proposition (`proposition_id` unique), tâches générées depuis les modèles `actif`, dans l'ordre `ordre`
- [x] Responsable de la tâche `MANAGER` : responsable du département du poste (`departement.responsable_id`)
- [x] `OnboardingTacheService.faire` : utilisateur du rôle responsable (ou `RH`), `fait_le` / `fait_par` renseignés
- [x] `OnboardingTacheService.annuler` : tâches non obligatoires seulement (`TACHE_OBLIGATOIRE`)
- [x] Fin automatique : toutes les tâches obligatoires `FAIT` → onboarding `TERMINE` + `date_fin`
- [ ] Tâche DSI « Créer le compte informatique » : crée l'`utilisateur` `SALARIE` et renseigne `employe.utilisateur_id` (point d'appel `CreationCompteSalarie` en place, implémentation : coordinateur)
- [x] `DocumentService` : dépôt (au moins un rattachement), `depose_par` = utilisateur courant, fichier en GED, pas de suppression
- [ ] Brancher sur le bloc 3 : acceptation → contrat `BROUILLON` + affectation `EMBAUCHE` + onboarding `EN_COURS`

## API

- [x] DTO contrat, avenant, affectation, modèle de tâche, onboarding (avec avancement), tâche, document
- [x] `GET/POST /api/employes/{id}/contrats`, `GET /api/contrats/{id}`, `POST /api/contrats/{id}/activer`, `/terminer`, `/rompre`
- [x] `POST /api/contrats/{id}/avenants`
- [x] `GET/POST /api/employes/{id}/affectations`, `GET /api/postes/{id}/affectations`
- [x] `GET/POST /api/modeles-taches`, `PUT /api/modeles-taches/{id}`
- [x] `POST /api/propositions/{id}/onboarding`, `GET /api/onboardings`, `GET /api/onboardings/{id}`
- [x] `GET /api/onboarding-taches/a-faire`, `POST /api/onboarding-taches/{id}/faire`, `/annuler`, `PATCH /api/onboarding-taches/{id}/responsable`
- [x] `POST /api/documents` (multipart), `GET /api/documents`, `GET /api/documents/{id}/fichier`

## Sécurité / droits

- [x] `@PreAuthorize` : contrats, avenants, affectations, onboarding → `RH` ; modèles de tâches → `ADMIN`, `RH`
- [x] Tâches : rôle responsable de la tâche (contrôle dans le service) — `DSI`, `PAIE`, `SERVICES_GENERAUX`, `MANAGER`, `RH`
- [x] `SALARIE` : lecture de ses contrats et de ses pièces, dépôt de ses pièces (onboarding et affectations : RH seulement, comme le contrat d'API)
- [x] Documents et salaires : accès limité par rôle (données personnelles)

## Tests

- [x] `ContratWorkflowTest`
- [x] `ContratServiceTest` : CDD sans date de fin refusé, deux contrats actifs refusés, renouvellement chaîné, rupture sans motif refusée, historique écrit
- [x] `AvenantServiceTest` : avenant sur contrat non actif refusé, contrat non modifié
- [x] `AffectationServiceTest` : poste `OCCUPE` refusé, poste passe `OCCUPE` avec historique, mutation ferme l'affectation précédente
- [x] `OnboardingServiceTest` : tâches générées depuis les modèles actifs, annulation d'une tâche obligatoire refusée, mauvais rôle refusé, fin automatique
- [x] `DocumentServiceTest` : document sans rattachement refusé

## Front Vue.js

- [ ] Dossier salarié : contrats successifs (frise CDD → renouvellement → CDI), avenants, affectations
- [ ] Création / activation / renouvellement / fin / rupture d'un contrat
- [ ] Création d'un avenant
- [ ] Affectation et mutation
- [ ] Écran admin des modèles de tâches (ordre, obligatoire, actif)
- [ ] Suivi des onboardings (RH) : avancement par employé
- [ ] « Mes tâches » par rôle (DSI, PAIE, SERVICES_GENERAUX, MANAGER, RH) : marquer fait
- [ ] Dépôt et liste des documents (employé, contrat, tâche)

## Démo

- [ ] Scénario : employé du bloc 3 → contrat CDD activé → affectation, poste `OCCUPE` → onboarding avec 6 tâches → chaque rôle fait sa tâche → onboarding `TERMINE` → renouvellement CDD puis CDI
- [ ] Contre-tests : annulation d'une tâche obligatoire, tâche faite par le mauvais rôle, deuxième contrat actif, affectation sur poste occupé
- [ ] Vérifier en base : `historique_contrat`, `historique_poste`, `fait_le` / `fait_par`, chaîne `contrat_precedent_id`
