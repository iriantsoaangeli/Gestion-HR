# Todo — Bloc 3 : Embauche

> Conception : [Conception.md — Bloc 3](Conception.md#bloc-3)
> État : tables dans `V1__init.sql` (`employe` déjà mappé en lecture au bloc 1), **tout le reste à faire**. Prérequis : bloc 2 (candidature `RETENUE`).

Ordre conseillé : proposition → génération du matricule → création de l'employé → clôture demande / offre → front.

## Base

- [x] Relire `proposition_embauche`, `employe`, `historique_proposition_embauche`, `historique_employe`
- [x] Choisir la génération du matricule (séquence PostgreSQL dédiée → migration V2, ou calcul applicatif) ; format proposé `EMP-<année>-<séquence>`
- [x] Données de test : 1 proposition par statut (`EN_ATTENTE`, `ACCEPTEE`, `REFUSEE`, `EXPIREE`), 1 employé issu d'une proposition acceptée

## Modèle / Repository

- [x] Enums `StatutProposition` (`EN_ATTENTE`, `ACCEPTEE`, `REFUSEE`, `EXPIREE`), `TypeContrat` (`CDD`, `CDI`, `STAGE`, `INTERIM`, partagé avec le bloc 4)
- [x] `PropositionEmbauche` + `HistoriquePropositionEmbauche` + repositories (package `model/embauche`)
- [x] Compléter `Employe` : `candidat_id`, `proposition_id`, `utilisateur_id`, `cree_par` en écriture (plus seulement en lecture)
- [x] `HistoriqueEmploye` + repository
- [ ] `EmployeRepository` : `existsByEmail`, recherche par statut / département (via affectation au bloc 4). Fait : `existsByEmail`, `existsByMatricule`, `findByPropositionId`, séquence du matricule, statut. Reste : département (bloc 4)

## Services & règles

- [x] `PropositionWorkflow` : `EN_ATTENTE → ACCEPTEE / REFUSEE / EXPIREE`, rôle `RH`
- [x] `PropositionService.creer` : candidature `RETENUE` (`CANDIDATURE_NON_RETENUE`), une seule proposition `EN_ATTENTE` / `ACCEPTEE` par candidature, `salaire > 0`, `date_expiration` future ; `null → EN_ATTENTE` historisé
- [x] `PropositionService.refuser` / `expirer` + historique ; refus d'accepter une proposition expirée (`PROPOSITION_EXPIREE`)
- [x] `MatriculeService` : génère un matricule unique, jamais réutilisé
- [x] `EmbaucheService.accepter` (une transaction) : proposition `→ ACCEPTEE`, création de l'`employe` (données du candidat, `date_embauche` = `date_debut`, `statut` `ACTIF`), `historique_employe` `null → ACTIF`
- [x] Email employé déjà utilisé → 422 `EMAIL_DEJA_UTILISE`
- [x] Mobilité interne : `candidat.employe_id` renseigné → pas de nouvel employé, il garde son matricule
- [ ] `DemandeWorkflow` : ajouter `RECRUTEMENT_OUVERT → CLOTUREE` (demande pourvue). Coordinateur (fichier partagé, contrat_api § 5)
- [ ] Effectif atteint (`demande_recrutement.effectif`) : offre `→ CLOTUREE`, demande `→ CLOTUREE`, autres candidatures non finales `→ REJETEE` motivées (à confirmer). Coordinateur, via une implémentation de `ActionApresEmbauche`
- [ ] Brancher le bloc 4 : l'acceptation déclenche (ou prépare) contrat, affectation et onboarding. Point d'appel prêt (`ActionApresEmbauche`, même transaction), branchement par le coordinateur
- [x] Trancher : qui valide le salaire ? contrôle par rapport au budget de la demande ?

## API

- [x] DTO `PropositionRequest` / `PropositionResponse`, `EmployeResponse` / `EmployeDetailResponse`
- [x] `POST /api/candidatures/{id}/propositions`, `GET /api/propositions`, `GET /api/propositions/{id}`
- [x] `POST /api/propositions/{id}/accepter` (renvoie l'employé créé), `/refuser`, `/expirer`
- [x] `GET /api/employes`, `GET /api/employes/{id}`

## Sécurité / droits

- [x] `@PreAuthorize` : propositions et embauche → `RH`
- [x] `GET /api/employes` → `RH`, `DIRECTION` (laissée ouverte à tout utilisateur identifié : le manager s'en sert au bloc 1 pour l'employé sortant ; format court sans e-mail ni salaire) ; `GET /api/employes/{id}` → `RH` ou l'employé lui-même (`employe.utilisateur_id`)
- [x] Salaire visible seulement par `RH`, `DIRECTION` et l'intéressé

## Tests

- [x] `PropositionWorkflowTest`
- [x] `PropositionServiceTest` : proposition sur candidature non retenue refusée, deuxième proposition en attente refusée, acceptation d'une expirée refusée
- [x] `EmbaucheServiceTest` : employé créé avec matricule et historique, pas d'employé sans proposition acceptée, mobilité interne sans nouvel employé, email en double refusé
- [x] `MatriculeServiceTest` : unicité, jamais réutilisé
- [ ] `DemandeWorkflowTest` : nouvelle transition `RECRUTEMENT_OUVERT → CLOTUREE` (coordinateur, avec la transition)

## Front Vue.js

- [ ] Depuis une candidature `RETENUE` : formulaire de proposition (salaire, date de début, type de contrat, date d'expiration)
- [ ] Liste des propositions (filtre statut), fiche avec historique
- [ ] Actions accepter / refuser / expirer ; après acceptation, redirection vers le dossier salarié
- [ ] Liste du personnel et dossier salarié (identité, matricule, statut)

## Démo

- [ ] Scénario : candidature `RETENUE` du bloc 2 → proposition → acceptation → employé créé avec matricule → demande et offre clôturées (effectif 1)
- [x] Contre-tests : proposition sur candidature `REJETEE`, acceptation d'une proposition expirée (tests Mockito, et essai `curl` sur `sirh_bloc3` : 422 attendus)
- [ ] Vérifier en base : `historique_proposition_embauche`, `historique_employe`, `historique_demande_recrutement` (`RECRUTEMENT_OUVERT → CLOTUREE`)
