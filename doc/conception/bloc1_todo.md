# Todo — Bloc 1 : Expression du besoin en personnel

> Conception : [Conception.md — Bloc 1](Conception.md#bloc-1)
> État au 22/10/2026 : **back et front faits et testés** (75 tests back ; scénario de démo rejoué dans le navigateur, sécurité activée et désactivée). Restent les points à trancher en équipe.
> Détail de la passe back : [`doc/todo/bloc1-dev.md`](../todo/bloc1-dev.md).

Flux : `BROUILLON → SOUMISE → VALIDEE_RH → VALIDEE_DIRECTION → RECRUTEMENT_OUVERT` ; `REFUSEE` depuis `SOUMISE` (RH) et `VALIDEE_RH` (Direction) ; `CLOTUREE` sur annulation depuis `BROUILLON` / `SOUMISE`.

## Base

- [x] Tables `role`, `utilisateur`, `departement`, `poste`, `demande_recrutement`, `validation` dans `V1__init.sql`
- [x] Historiques `historique_departement`, `historique_poste`, `historique_demande_recrutement`
- [x] Contraintes : statuts `CHECK`, `CREATION` sans `employe_sortant_id`, refus motivé sur `validation`, `budget >= 0`, `effectif > 0`
- [x] Données de test : migration Flyway `V2__donnee_de_test.sql` (copie de `backend/scripts/data.sql`) ; `dataReset.sql`, `insert.sh`, `reset.sh` pour les remettre

## Modèle / Repository

- [x] `Role` + `CodeRole`, `Utilisateur`, `UtilisateurRepository`
- [x] `Departement`, `HistoriqueDepartement` + repositories
- [x] `Poste`, `StatutPoste`, `HistoriquePoste` + repositories
- [x] `DemandeRecrutement`, `TypeDemande`, `StatutDemande`, `HistoriqueDemandeRecrutement` + repositories
- [x] `Validation`, `NiveauValidation`, `Decision` + repository
- [x] `Employe`, `StatutEmploye` (lecture seule, complété au bloc 3) + repository
- [x] `contextLoads` valide toutes les entités contre V1 (`ddl-auto: validate`)

## Services & règles

- [x] `DepartementService` : lister, détail, créer (nom unique), modifier (historise budget / responsable), `estResponsable`
- [x] Responsable = utilisateur actif de rôle `MANAGER` (`RESPONSABLE_INVALIDE`)
- [x] `PosteService` : lister filtrable, créer (`VACANT`), `changerStatut` + `historique_poste`
- [x] Règles poste : `SUPPRIME` figé, `OCCUPE → SUPPRIME` interdit, suppression motivée, même statut refusé
- [x] `DemandeWorkflow` : table statut × rôle → statuts suivants, `TRANSITION_INTERDITE`
- [x] `DemandeService` : créer, modifier (`BROUILLON` + demandeur), soumettre (règles revérifiées), annuler, mes demandes, détail ; `changerStatut` unique point de passage
- [x] Règles demande : responsable du département, poste non supprimé, `CREATION` sans sortant, budget ≤ budget du département
- [x] `ValidationService` : décision RH / Direction (niveau déduit du rôle), refus motivé, `BUDGET_ABSENT`, ouverture du recrutement, file à traiter
- [ ] Trancher : annulation après `VALIDEE_RH` ? (aujourd'hui interdite)
- [ ] Trancher : déduire les demandes en cours du budget du département ?
- [ ] Trancher : employé sortant obligatoirement `ACTIF` (et occupant du poste) ?
- [ ] Préparer le bloc 3 : transition `RECRUTEMENT_OUVERT → CLOTUREE` dans `DemandeWorkflow` (demande pourvue)
- [x] `poste.classification` remplacée par `poste.role_id` (migration V3) : un rôle pour plusieurs postes ; rôle inconnu → 422 `ROLE_INVALIDE`
- [ ] Blocs 3-4 : une personne n'occupe qu'un poste à la fois (une seule `affectation` sans `date_fin` par employé)

## API

- [x] `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/moi`
- [x] `GET/POST /api/departements`, `GET/PUT /api/departements/{id}`
- [x] `GET/POST /api/postes`, `PATCH /api/postes/{id}/statut`
- [x] `POST /api/demandes`, `PUT /{id}`, `POST /{id}/soumettre`, `POST /{id}/annuler`, `GET /mes-demandes`, `GET /{id}`
- [x] `POST /api/demandes/{id}/validations`, `POST /api/demandes/{id}/ouvrir-recrutement`, `GET /api/validations/a-traiter`
- [x] Erreurs JSON `{code, message, details}` (`GlobalExceptionHandler`)
- [x] Swagger (`/swagger-ui.html`)
- [x] `GET /api/departements/{id}/historique` → `HistoriqueDepartementResponse`
- [x] `GET /api/postes/{id}/historique` → `HistoriquePosteResponse` (`HistoriqueStatutResponse` est typé sur les statuts de demande)
- [x] `GET /api/postes/{id}` (fiche d'un poste)
- [x] `GET /api/demandes?statut=` (`RH`, `DIRECTION`, `ADMIN`) pour le tableau de bord
- [x] Listes en lecture pour les formulaires du front : `GET /api/employes?statut=` (employé sortant), `GET /api/utilisateurs?role=` (responsable, logins affichés)

## Sécurité / droits

- [x] Sécurité activable (`SECURITE_ACTIVE`), session, BCrypt, `UtilisateurCourant` (session ou `X-Utilisateur-Id`)
- [x] `@PreAuthorize` : départements / postes en écriture → `ADMIN` / `RH` ; demandes → `MANAGER` ; validations → `RH` / `DIRECTION` ; ouverture → `RH`
- [x] Règles liées au rôle aussi dans les services (actives dans les deux modes)
- [ ] Trancher : restreindre `GET /api/demandes/{id}` au demandeur, à la RH et à la Direction
- [x] Front servi via le proxy Vite (même origine, cookie de session sans CORS) ; CORS configurable par `CORS_ORIGINES` (`CorsConfig`), désactivé par défaut
- [ ] CSRF désactivé : à revoir si l'application est exposée

## Tests

- [x] Unitaires Mockito : `DepartementServiceTest`, `PosteServiceTest`, `DemandeServiceTest`, `ValidationServiceTest`, `GlobalExceptionHandlerTest`
- [x] `DemandeWorkflowTest` : transitions autorisées et interdites, statuts finaux
- [x] `SirhApplicationTests.contextLoads`
- [x] Tests des contrôleurs (`ControleursTest`, MockMvc) : codes HTTP, `@PreAuthorize` avec sécurité active, format des erreurs
- [x] Tests des routes d'historique et de la liste des demandes (services + MockMvc)

## Front Vue.js

- [x] Outillage : Vite + Vue 3 + Vue Router, JavaScript, `fetch`, CSS maison (`frontend/`, voir `frontend/README.md`)
- [x] Client API (`api.js`) : cookie de session ou en-tête `X-Utilisateur-Id`, erreurs `{code, message, details}` affichées (`MessageErreur`)
- [x] Connexion / déconnexion dans les deux modes de sécurité (détectés par `GET /api/moi`), en-tête avec l'utilisateur courant, menus selon le rôle
- [x] Départements : liste, fiche (avec postes), création / modification (`ADMIN`, `RH`)
- [x] Postes : liste filtrable (département, statut), création, changement de statut avec commentaire (obligatoire pour une suppression)
- [x] Historique d'un département et d'un poste
- [x] Formulaire de demande (`MANAGER`) : postes de ses départements seulement, type, employé sortant si `REMPLACEMENT`, effectif, motif, budget (budget du département affiché), date souhaitée ; « enregistrer » ou « enregistrer et soumettre »
- [x] « Mes demandes » : liste avec statut, actions modifier / soumettre / annuler selon le statut
- [x] Fiche détail d'une demande : décisions + historique des statuts, actions selon le rôle
- [x] File « À traiter » RH : demandes `SOUMISE` à valider / refuser (refus motivé), demandes `VALIDEE_DIRECTION` dont ouvrir le recrutement
- [x] File « À traiter » Direction : demandes `VALIDEE_RH` (budget demandé / budget du département, motif), valider / refuser
- [x] Tableau de bord (`RH`, `DIRECTION`, `ADMIN`) : compteurs par statut, budget engagé, liste filtrable

## Démo

- [x] Scénario joué avec curl (sécurité active) : création + soumission par `manager1`, validation `rh1`, refus sans commentaire (422) puis validation `direction1`, ouverture par `rh1`
- [x] Contre-tests : `manager2` sur le département de `manager1` refusé, RH au niveau Direction refusé
- [x] Même scénario rejoué depuis le front (navigateur automatisé, sécurité activée) : budget dépassé refusé, `manager2` limité à Commercial, refus sans commentaire bloqué, fiche finale avec 2 décisions et 5 lignes d'historique ; connexion sans sécurité vérifiée aussi
- [ ] Nettoyer la doc restée sur l'ancien journal d'audit : `doc/tables/journal_audit.html`, `doc/tables/index.html`, `doc/tables/utilisateur.html`, `doc/tests/*.html`
- [ ] Décider si `doc/apjapi*` est commité ou ignoré
