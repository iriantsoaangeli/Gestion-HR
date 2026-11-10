# Todo — Bloc 5 : Dossier administratif du salarié

> Source : diapo du cours [`bloc5.jpg`](../cours/bloc5.jpg) (le PDF ne couvre que les blocs 1 à 4).
> État : **fait le 08/11/2026** (back, front, tests, données de test). Écarts avec cette todo : [blocs5-7_explications.md](blocs5-7_explications.md#écarts-avec-les-todos-choix-faits-en-codant). Guide : [blocs5-7_guide.md](blocs5-7_guide.md). Prérequis : bloc 3 (employé + matricule) et bloc 4 (contrats, avenants, affectations, documents).
> Migration proposée : **V14** (V13 est réservée à [todofix](../problemes/todofix.md)).

Ce que demande le cours :
1. Centraliser identité, coordonnées, documents et situation professionnelle.
2. Conserver contrats, affectations, salaire et historique de carrière.
3. Gérer les documents administratifs et leur validité.
4. Prévoir confidentialité, habilitations et journal d'audit.
5. Le dossier salarié devient le **référentiel central** du SIRH (les blocs 6 et 7 s'y rattachent).

## Ce qui existe déjà (à réutiliser, pas à refaire)

| Besoin du cours | Déjà là | Manque |
|---|---|---|
| Identité | `employe` : matricule, nom, prénom, email, téléphone, date de naissance, date d'embauche, statut | sexe, CIN, adresse, situation familiale, contact d'urgence… ; **aucune modification possible** |
| Statut du salarié | `StatutEmploye` ACTIF / SUSPENDU / SORTI + `historique_employe` | **aucun workflow ni route** pour suspendre, réintégrer, faire sortir |
| Contrats, salaire | `contrat`, `avenant`, `historique_contrat`, `salaireCourant` | vue « carrière » consolidée |
| Affectations | `affectation` (lignes datées) | poste et département **courants** dans la liste du personnel |
| Documents | `document` (GED locale, types CV, RIB, PIECE_IDENTITE…) | date de validité, état (valide / expiré / remplacé), alertes |
| Droits | `AccesDossierSalarie` (RH écrit ; DIRECTION, ADMIN lisent) | niveaux de confidentialité (salaire, pièces), accès du manager à son équipe, PAIE |
| Journal d'audit | **retiré volontairement** (Conception, principes transverses) | le cours le redemande → décision ci-dessous |

Rappel : les salariés **n'ont pas de compte** (V11). Le dossier est tenu par la RH ; aucun self-service.

## À trancher (proposition par défaut si rien n'est décidé)

| # | Question | Proposition par défaut |
|---|---|---|
| D1 | Journal d'audit : on le remet ? | **Oui, limité** : table `journal_audit` pour (a) les **consultations** de données sensibles (salaire, pièces, dossier complet) et (b) les modifications de l'identité / coordonnées. Les changements de statut restent dans les `historique_*`. Le cours l'exige (« prévoir journal d'audit », « Audit & logs »). |
| D2 | Modifier l'identité : on écrase ou on historise ? | Mise à jour de `employe` **+ une ligne par champ modifié** dans `historique_modification_employe` (champ, ancienne valeur, nouvelle valeur, qui, quand, motif). Principe « historiser plutôt qu'écraser ». |
| D3 | Qui modifie le dossier ? | RH seule. DIRECTION et ADMIN en lecture. |
| D4 | Le manager voit-il ses salariés ? | Oui, **sans le salaire ni les pièces** : identité, poste, contrat (type, dates), statut — seulement pour les employés affectés à un poste de **son** département. |
| D5 | PAIE voit quoi ? | Identité, matricule, contrat actif, salaire courant, RIB (pour la paie, blocs 6-7). Pas les autres pièces. |
| D6 | Validité des documents : quels types expirent ? | `PIECE_IDENTITE`, `CERTIFICAT_MEDICAL`, `PERMIS`, `TITRE_SEJOUR` ont une `date_expiration` obligatoire ; les autres non. Alerte à **30 jours** (constante). |
| D7 | Un document expiré est-il supprimé ? | Non. Il passe `EXPIRE` (calculé) ou `REMPLACE` quand une nouvelle pièce du même type est déposée (`remplace_par_id`). |
| D8 | Sortie du salarié (SORTI) : effets ? | Motif + date de sortie obligatoires ; refusée tant qu'un contrat est `ACTIF` (il faut le terminer / rompre d'abord) ; ferme l'affectation ouverte (`date_fin`) ; le poste repasse `VACANT` (via `ChangementStatutPoste`). Sortie finale. |
| D9 | Suspension : effets ? | Motif obligatoire, aucune autre conséquence au MVP (le contrat reste actif). Bloque la saisie de pointage et de congé (blocs 6-7). |

## Base (V14)

- [x] Relire `employe`, `historique_employe`, `document`, `contrat`, `avenant`, `affectation` (V1, V10, V11, V12)
- [x] `employe` : colonnes `sexe` (`M`/`F`), `cin` (UNIQUE, 12 chiffres à Madagascar), `cin_delivre_le`, `cin_lieu`, `adresse`, `ville`, `situation_familiale` (`CELIBATAIRE`, `MARIE`, `DIVORCE`, `VEUF`), `nombre_enfants` (≥ 0), `contact_urgence_nom`, `contact_urgence_telephone`, `date_sortie`, `motif_sortie` ; CHECK `statut <> 'SORTI' OR date_sortie IS NOT NULL`, `date_sortie >= date_embauche`
- [x] `historique_modification_employe` (employe_id, champ, ancienne_valeur, nouvelle_valeur, motif, utilisateur_id, date_action) — colonnes non modifiables
- [x] `document` : `date_delivrance`, `date_expiration`, `numero`, `remplace_par_id` (FK `document`) ; CHECK `date_expiration >= date_delivrance` ; nouveaux types `CERTIFICAT_MEDICAL`, `PERMIS`, `TITRE_SEJOUR`, `ATTESTATION`, `JUSTIFICATIF_ABSENCE` (ce dernier pour le bloc 7)
- [x] (D1) `journal_audit` (utilisateur_id, action `CONSULTATION`/`MODIFICATION`/`TELECHARGEMENT`, ressource, ressource_id, employe_id, detail, adresse_ip, date_action) ; index sur `employe_id`, `date_action`
- [x] Données de test `scripts/data_bloc5.sql` : compléter les employés existants (CIN, adresse…), une pièce d'identité qui expire dans 15 jours, une expirée, un salarié suspendu, un salarié sorti

## Modèle / Repository

- [x] Compléter `Employe` (nouvelles colonnes) ; enums `Sexe`, `SituationFamiliale`
- [x] `HistoriqueModificationEmploye` + repository
- [x] `Document` : nouveaux champs ; `TypeDocument` : nouveaux types ; méthode `etat(aujourdhui)` → `VALIDE`, `EXPIRE_BIENTOT`, `EXPIRE`, `REMPLACE` (calculé, pas stocké)
- [x] `JournalAudit` + repository (D1)
- [x] `EmployeRepository` : recherche par statut, département, poste, texte (nom, matricule, CIN) via l'affectation ouverte
- [x] `contextLoads` passe

## Services & règles

- [x] `EmployeWorkflow` : `ACTIF → SUSPENDU | SORTI`, `SUSPENDU → ACTIF | SORTI`, `SORTI` final ; rôle RH
- [x] `EmployeService.changerStatut` : motif obligatoire (`MOTIF_OBLIGATOIRE`), `historique_employe` dans la même transaction
- [x] Sortie (D8) : `CONTRAT_ACTIF_EXISTANT` si un contrat est actif ; ferme l'affectation ; poste → `VACANT` ; `date_sortie`, `motif_sortie`
- [x] `EmployeService.modifier` (identité, coordonnées) : RH seule ; refus si `SORTI` (`DOSSIER_CLOS`) ; email et CIN uniques (`EMAIL_DEJA_UTILISE`, `CIN_DEJA_UTILISE`) ; matricule et date d'embauche **non modifiables** ; une ligne d'historique par champ changé (D2) ; motif facultatif
- [x] Règles : âge ≥ 18 ans à l'embauche (`AGE_MINIMUM`), téléphone via `RegleTelephone`, CIN 12 chiffres (`CIN_INVALIDE`)
- [x] `SituationProfessionnelle` (lecture) : poste et département courants, contrat actif (type, dates, fin de période d'essai), salaire courant, ancienneté (années / mois depuis `date_embauche`)
- [x] `CarriereService.frise(employeId)` : événements datés triés (embauche, contrats, renouvellements, avenants de salaire, affectations / mutations / promotions, suspensions, sortie), **calculés** à partir des tables existantes (aucune table en plus)
- [x] `DocumentService` : `date_expiration` obligatoire pour les types de D6 (`DATE_EXPIRATION_OBLIGATOIRE`) ; un nouveau dépôt du même type pour le même employé marque l'ancien `REMPLACE` (D7) ; liste des pièces expirées ou expirant sous 30 jours
- [x] Pièces obligatoires du dossier (constante) : `PIECE_IDENTITE`, `CONTRAT_SIGNE`, `RIB` → indicateur « dossier complet / pièces manquantes »
- [x] Habilitations : étendre `AccesDossierSalarie` avec un niveau par section (identité, salaire, pièces) selon D3 à D5 ; masquer `salaire` (`null`) dans les DTO pour qui n'y a pas droit, comme le budget des départements (contrat_api § 6)
- [x] Journal d'audit (D1) : composant `JournalAudit.consigner(...)` appelé par le service lors d'une consultation du dossier complet, du salaire, d'un téléchargement de pièce ou du PDF de contrat, et à chaque modification d'identité

## API

- [x] `GET /api/employes?statut=&departementId=&posteId=&q=` (RH, DIRECTION, ADMIN ; MANAGER = son département) → liste enrichie (poste, département, contrat actif, dossier complet ?)
- [x] `GET /api/employes/{id}` → `EmployeDetailResponse` étendu (identité, coordonnées, situation professionnelle, pièces manquantes) ; champs masqués selon le rôle
- [x] `PUT /api/employes/{id}` (RH) `EmployeModificationRequest` → `EmployeDetailResponse`
- [x] `POST /api/employes/{id}/suspendre`, `/reintegrer`, `/sortir` (RH) `{commentaire, dateSortie?}` → `EmployeDetailResponse`
- [x] `GET /api/employes/{id}/carriere` → `[EvenementCarriere {date, type, libelle, detail, lien}]`
- [x] `GET /api/employes/{id}/modifications` (RH, DIRECTION, ADMIN) → historique des champs
- [x] `GET /api/documents/a-renouveler?jours=30` (RH) → pièces expirées / expirant bientôt
- [x] `GET /api/journal-audit?employeId=&utilisateurId=&du=&au=` (ADMIN, DIRECTION) → journal paginé
- [x] Mettre à jour `contrat_api.md` (nouveau § 9) et les codes d'erreur

## Sécurité / droits

- [x] `@PreAuthorize` sur les nouvelles routes + mêmes règles dans les services
- [x] Tableau des habilitations (à mettre dans `Conception.md`) : rôle × section (identité, coordonnées, salaire, pièces, carrière, journal)
- [x] MANAGER : filtre sur son département dans la liste et la fiche (`ACCES_REFUSE` sinon)
- [x] Le journal d'audit n'est ni modifiable ni supprimable (aucune route d'écriture)

## Tests

- [x] `EmployeWorkflowTest` : transitions et rôles
- [x] `EmployeServiceTest` : sortie refusée avec contrat actif, sortie ferme l'affectation et libère le poste, suspension sans motif refusée, modification historisée champ par champ, CIN / email en double refusés, dossier sorti non modifiable
- [x] `DocumentServiceTest` : date d'expiration obligatoire, remplacement d'une pièce, liste « à renouveler »
- [x] `CarriereServiceTest` : frise CDD → renouvellement → avenant → mutation → CDI dans l'ordre
- [x] `AccesDossierSalarieTest` : salaire masqué pour MANAGER, manager d'un autre département refusé, PAIE voit le RIB
- [x] Journal : une consultation du salaire écrit une ligne

## Front Vue.js

- [x] Liste du personnel : filtres statut / département / poste, recherche nom-matricule-CIN, colonnes poste et département courants, badge « dossier incomplet »
- [x] Dossier salarié en onglets : **Identité & coordonnées** · **Situation professionnelle** · **Carrière** (frise) · **Documents** (avec état de validité) · **Historique**
- [x] Formulaire de modification (RH) avec motif ; historique des modifications
- [x] Actions Suspendre / Réintégrer / Faire sortir (Dialogue de confirmation, motif obligatoire)
- [x] Carte « Pièces à renouveler » dans « À faire » de la RH (compteur)
- [x] Écran « Journal d'audit » (ADMIN, DIRECTION) : tableau filtrable
- [x] Masquage du salaire et des pièces selon le rôle (même règle que le back)

## Démo

- [x] Scénario : salarié issu du bloc 4 → compléter CIN et adresse (historisé) → déposer pièce d'identité avec date d'expiration → alerte à 30 jours → nouvelle pièce, l'ancienne passe « remplacée » → avenant de salaire visible dans la carrière → mutation → suspension puis réintégration → fin de contrat puis sortie, poste vacant
- [x] Contre-tests : sortie avec contrat actif, manager d'un autre département, modification du matricule, CIN en double
- [x] Vérifier en base : `historique_employe`, `historique_modification_employe`, `journal_audit`, `document.remplace_par_id`

## Branchements avec les blocs 6 et 7

- Le statut du salarié conditionne tout : `SUSPENDU` ou `SORTI` → plus de pointage (bloc 6) ni de demande de congé (bloc 7).
- La situation professionnelle donne au bloc 6 le contrat actif (`heures_hebdo`) et le département (manager responsable).
- Le type `JUSTIFICATIF_ABSENCE` sert au bloc 7.
