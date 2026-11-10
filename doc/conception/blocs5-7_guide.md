# Guide pratique des blocs 5 à 7

> Comment **faire** chaque opération (dossier, temps, congés), dans quel ordre, avec quel compte, ce qu'il faut voir à l'écran et en base.
> Ce que veut dire chaque morceau : [blocs5-7_explications.md](blocs5-7_explications.md).

## 0. Préparer la démo

```bash
cd backend && ./scripts/reset.sh -y --insert   # vide la base, remet les données des blocs 1 à 7, copie les pièces de démo dans ged/demo
cd .. && ./run.sh                              # API (8081) + front (5173) ; Flyway applique V14, V15, V16 au premier lancement
```

- Mode démo (sécurité désactivée) : on choisit le compte par son login, sans mot de passe.
- Les dates des données sont **relatives à aujourd'hui** : la démo marche quel que soit le jour.
- Les journées, anomalies et heures sup. ne sont pas dans les données : elles sont **calculées**. Premier geste : `rh1` → **Pointage** → « Recalculer une période » du lundi de la semaine dernière à aujourd'hui.

| Compte | id | Ce qu'il fait ici |
|---|---|---|
| `rh1` | 3 | tout : dossier, pointage, import, validation RH, clôture, paramètres |
| `manager1` | 1 | équipe Informatique + Finance (Jean, Lova, Fara, Hery) : pointe, valide congés et HS de son équipe |
| `manager2` | 2 | équipe Commercial (Marie) |
| `direction1` | 4 | lit tout, journal d'audit, valide un congé quand il n'y a pas de manager |
| `admin` | 5 | lit tout, journal d'audit |
| `paie1` | 8 | dossiers sans pièces (sauf RIB), variables de paie, export CSV |

Salariés de test : **Jean** Rakoto (EMP-0001, Informatique, dossier incomplet), **Marie** Rabe (EMP-0002, Commercial, pièces qui expirent), **Hery** Andria (Finance, dossier complet), **Fara** Ravelo (suspendue), **Toky** Rajaona (sorti), Lova Rasoa (contrat en brouillon).

---

## 1. Bloc 5 — dossier du salarié

### 1.1 Lire un dossier et ses droits

1. `rh1` → **Personnel** : colonnes poste, département, contrat, badge « incomplet » (survol : pièces manquantes). Filtre par département, recherche par nom, matricule ou CIN.
2. Ouvrir Jean : onglets **Identité** (alerte « dossier incomplet »), **Situation et contrats**, **Carrière**, **Documents**, **Temps**, **Congés et absences**, **Historique**.
3. Même dossier avec `manager1` : pas de CIN, d'adresse, de salaire, pas d'onglet Documents.
4. `manager1` ouvre Marie (autre équipe) : **403**. Ce refus apparaît dans le journal d'audit.
5. `paie1` ouvre Marie → Documents : seulement le RIB.

### 1.2 Modifier l'identité (historisé champ par champ)

`rh1`, dossier de Jean → **Modifier** → changer l'adresse, mettre une CIN « 123 » (acceptée en mode démo), motif « Déménagement » → Enregistrer.
- Onglet **Historique** : une ligne par champ (avant → après, motif, auteur).
- Contre-tests : CIN de Marie (`201021023456`) → `CIN_DEJA_UTILISE` ; e-mail de Marie → `EMAIL_DEJA_UTILISE` ; sur Toky (sorti) le bouton n'existe plus (API : `DOSSIER_CLOS`).
- Voir le contrôle strict : relancer l'API avec `SIRH_CONTROLES_STRICTS=true` → CIN « 123 » refusée (`CIN_INVALIDE`).

### 1.3 Pièces et validité

1. `rh1` → **Pièces à renouveler** : le permis de Marie (expiré) et son certificat médical (expire dans 15 jours).
2. Clic → dossier de Marie, onglet Documents → **Déposer une pièce** : type Certificat médical, date d'expiration dans un an.
3. L'ancien certificat passe **Remplacée** ; il disparaît de « Pièces à renouveler ». Il reste téléchargeable.
4. Jean : déposer un « Contrat signé » → le badge « incomplet » disparaît de la liste du personnel.

### 1.4 Suspendre, réintégrer, faire sortir

1. Jean → **Suspendre** (motif obligatoire). Ensuite : pointage et demande de congé refusés (`EMPLOYE_NON_ACTIF`).
2. **Réintégrer** → ACTIF. L'onglet Carrière montre la suspension et la réintégration.
3. **Faire sortir** Jean → refusé : `CONTRAT_ACTIF_EXISTANT`. Il faut d'abord terminer ou rompre son contrat (Situation et contrats → contrat → Rompre).
4. Après la sortie : affectation fermée, poste « Administrateur systèmes » VACANT, planning fermé, demandes de congé en cours annulées (solde recrédité).

### 1.5 Journal d'audit

`admin` ou `direction1` → **Journal d'audit** : filtrer par action (Consultation, Téléchargement, Modification, Export, Accès refusé) ou par salarié. `rh1` n'y a pas accès.

```sql
SELECT date_action, utilisateur_id, action, ressource, employe_id, detail FROM journal_audit ORDER BY id DESC LIMIT 20;
DELETE FROM journal_audit;   -- refusé par le déclencheur : le journal ne s'efface pas
```

---

## 2. Bloc 6 — temps de travail

### 2.1 Comprendre la semaine de test (semaine dernière)

Après le recalcul (§ 0), `rh1` → dossier de Jean → onglet **Temps**, mois précédent si besoin :

| Jour | Pointages | Résultat |
|---|---|---|
| lundi | 08:25 – 12:00, 13:00 – 17:00 | retard 25 min (au-delà de la tolérance de 10 min) |
| mardi | 07:58 – 12:01, 13:02 – … | sortie manquante |
| mercredi | 08:00 – 17:00 | normale : la pause non pointée (1 h) est déduite |
| jeudi | 08:00, 08:01, 12:00, 13:00, 17:00 | doublon (08:01 ignoré) |
| vendredi | rien | absence → **absence à justifier** créée au bloc 7 |

Marie : 9 h 30 par jour → **7 h 30 d'heures sup.** à valider. Hery : vendredi sortie à 15:30 → départ anticipé.

### 2.2 Traiter les anomalies

`rh1` (ou `manager1` pour Jean) → **Anomalies de pointage** :
- Sortie manquante de Jean : dossier → Temps → **Saisir un pointage** (SORTIE 17:00 le mardi). L'anomalie passe **Corrigée** toute seule.
- Retard de Jean : soit **Corriger** le pointage 08:25 → 08:05 avec motif (l'ancien reste, barré), soit **Justifier** (« embouteillage »).
- Doublon : **Ignorer** (« double badge »).
- Départ de Hery : **Justifier** (« rendez-vous médical »).
- Contre-tests : `manager2` ne peut rien faire sur Jean (403) ; motif vide → refusé.

### 2.3 Saisir et importer

- **Pointage** → « Saisir un pointage » : refus si le salarié est suspendu (Fara), sans planning (Lova : `PLANNING_ABSENT`), dans le futur, ou en double.
- Import (RH) : fichier `pointages.csv`, par exemple (mettre des dates passées de la semaine en cours) :

```text
matricule;horodatage;type
EMP-0002;2026-11-06 18:30;ENTREE
EMP-0002;2026-11-06 19:00;SORTIE
EMP-9999;2026-11-06 08:00;ENTREE
```

Le rapport dit, ligne par ligne, ce qui est importé ou rejeté (matricule inconnu, doublon, salarié suspendu…). Réimporter le même fichier ne crée rien.

### 2.4 Heures supplémentaires

`manager2` → **Heures sup. à valider** → semaine de Marie : 7 h 30 au 1er taux → **Valider**. `manager1` ne la voit pas. Refuser exige un motif. Si un pointage change ensuite la semaine, elle repasse « À valider ».

### 2.5 Régler les paramètres (D4, D6)

`rh1` → **Paramètres RH** :
- Onglet Paramètres → « Taux des premières heures supplémentaires » → 125 → la prochaine semaine calculée prend 125 % ; une semaine déjà calculée garde son taux jusqu'à son recalcul.
- Onglet Régimes : modifier la **tolérance de retard** du régime Bureau (ex. 30 min), puis **Recalculer** : le retard de 25 min disparaît.
- Durée légale : un régime de 45 h est refusé (`REGIME_AU_DELA_DUREE_LEGALE`).
- Les autres rôles voient les valeurs, pas le bouton.

### 2.6 Clôturer le mois et sortir la paie

`rh1` → **Mois et variables de paie** :
1. Le mois de la semaine de test montre ce qui bloque (anomalies ouvertes, HS à valider, absences à justifier).
2. **Clôturer** tant qu'il en reste → message qui dit quoi faire (`ANOMALIES_OUVERTES`, `HS_A_VALIDER`, `ABSENCES_A_JUSTIFIER`). Le mois en cours ne se clôture pas (`MOIS_NON_TERMINE`).
3. Tout traiter (§ 2.2, 2.4, et § 3.4 pour l'absence de Jean), puis clôturer : plus aucune saisie possible sur ce mois (`PERIODE_CLOTUREE`).
4. **Variables** → tableau par salarié ; `paie1` → **Exporter en CSV** (tracé dans le journal).

---

## 3. Bloc 7 — congés et absences

### 3.1 Soldes et droits

Dossier → onglet **Congés et absences** : soldes (acquis, pris, en attente, disponible), droits acquis mois par mois. `rh1` → **Ajuster** (+1,5 j « fractionnement », motif obligatoire ; un retrait qui rendrait le solde négatif est refusé).
Crédit du mois : **Paramètres RH** → « Crédit des congés » → mois terminé → Créditer. Relancer le même mois ne crédite rien (« déjà crédité »).

### 3.2 Une demande de bout en bout

1. `manager1` → **Congés** → **Nouvelle demande** → Jean, Congé payé, 3 jours en décembre : la **simulation** affiche les jours décomptés et le solde après.
2. La demande est **Soumise** ; le solde de Jean montre ces jours « en attente ».
3. `manager1` → fiche → **Valider (manager)** → Validée manager.
4. `rh1` → **Congés à valider** → **Valider (RH)** → Validée : solde « pris », absence « Congé » créée, journées en CONGE dans l'onglet Temps, case pleine dans le **Calendrier d'équipe**.
5. `rh1` → **Annuler** (avant le premier jour, motif) → solde recrédité, absence annulée.

Données prêtes : Marie (soumise, pour `manager2`), Jean (validée manager, pour `rh1`), Hery (validée, à annuler), une refusée, une annulée.

### 3.3 Contre-tests de la saisie

| Essai | Résultat |
|---|---|
| 40 jours pour Marie | `SOLDE_INSUFFISANT` (en attente compris) |
| Dates qui chevauchent une autre demande ou une absence | `CONGE_CHEVAUCHE` |
| Permission exceptionnelle de 12 jours | `PLAFOND_ANNUEL_DEPASSE` (10 j par an) |
| Congé pour Fara (suspendue) | `EMPLOYE_NON_ACTIF` |
| Congé pour Lova (pas de contrat actif) | `HORS_CONTRAT` |
| `manager1` saisit un congé déjà commencé | `DATE_PASSEE` (la RH, elle, peut régulariser) |
| `manager1` saisit pour Marie | 403 |
| `rh1` valide une demande Soumise | `TRANSITION_INTERDITE` (le manager d'abord) |
| `direction1` valide la demande de Jean | `VALIDATION_PAR_LE_MANAGER` (Jean a un manager) |
| Refus sans motif | `REFUS_NON_MOTIVE` |

### 3.4 Absences

- Jour sans pointage de Jean (vendredi de la semaine de test) : **Absences** → « À justifier » →
  - **Justifier** avec un fichier → Justifiée, la pièce rejoint le dossier (type « Justificatif d'absence ») ;
  - ou (`rh1`) **Non justifiée** → retenue d'un jour dans les variables de paie ;
  - ou saisir les pointages oubliés de ce jour : l'absence s'**annule toute seule**.
- **Saisir une absence** maladie à venir pour Marie (`manager2`) → « À justifier » ; un congé sur ces dates est refusé (`CONGE_CHEVAUCHE`).
- Une absence venue d'un congé ne s'annule pas ici (`ABSENCE_DE_CONGE`) : on annule la demande.

### 3.5 Calendrier d'équipe

`manager1` → **Calendrier d'équipe** : son équipe ; un département d'un autre manager est refusé. Pleine = congé validé, bordée bleue = en cours de validation, rouge = absence à justifier ou non justifiée.

---

## 4. Vérifier en base

```sql
-- Bloc 5
SELECT champ, ancienne_valeur, nouvelle_valeur, motif FROM historique_modification_employe ORDER BY id DESC;
SELECT id, type, nom_fichier, date_expiration, remplace_par_id FROM document WHERE employe_id IS NOT NULL ORDER BY id;
-- Bloc 6
SELECT e.matricule, j.date, j.statut, j.minutes_prevues, j.minutes_travaillees, j.minutes_retard
  FROM journee_travail j JOIN employe e ON e.id = j.employe_id ORDER BY j.date, e.matricule;
SELECT id, horodatage, type, source, annule, motif_annulation, corrige_id FROM pointage WHERE annule OR source = 'CORRECTION';
SELECT a.type, a.statut, h.ancien_statut, h.nouveau_statut, h.commentaire
  FROM anomalie_pointage a JOIN historique_anomalie_pointage h ON h.anomalie_id = a.id ORDER BY h.id;
SELECT semaine, minutes_total, minutes_palier1, taux_palier1, statut FROM heure_supplementaire;
SELECT code, valeur FROM parametre;  SELECT * FROM historique_parametre;
-- Bloc 7 : le cache des soldes doit égaler le recalcul (bouton « recalculer » côté API)
SELECT e.matricule, s.acquis, s.pris, s.en_attente, s.acquis - s.pris - s.en_attente AS disponible
  FROM solde_conge s JOIN employe e ON e.id = s.employe_id;
SELECT id, type, statut, demande_conge_id, anomalie_id, justificatif_id FROM absence ORDER BY id;
SELECT demande_conge_id, ancien_statut, nouveau_statut, commentaire FROM historique_demande_conge ORDER BY id;
```

## 5. Si quelque chose ne marche pas

| Symptôme | Cause probable |
|---|---|
| Onglet Temps vide | Pas de planning, ou pas encore recalculé : « Recalculer » |
| `PLANNING_ABSENT` | Affecter un régime au salarié (onglet Temps → « Affecter un régime ») |
| `PERIODE_CLOTUREE` | Le mois est clôturé : définitif, c'est voulu |
| Téléchargement d'une pièce de démo → 404 | Lancer l'API depuis `backend/` (GED = `backend/ged`) ou refaire `reset.sh -y --insert` |
| `MAUVAIS_ROLE` sur un paramètre | Seule la RH modifie les paramètres |
| Flyway se plaint de V13 | Normal : V13 (todofix) arrivera après V16, `out-of-order` est activé |
