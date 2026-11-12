## Workflow d'une demande 
> **Manager -> HR -> Direction -> Finance**

## Comptes utilisateurs
- Un compte = 1 poste d'importance qui utilise l'application (manager, responsable RH, direction, finance, administrateur).
- Un poste a au plus un compte ; un compte a toujours un poste.
- Un compte peut être désactivé (status = false) sans être supprimé ; chaque changement de statut ou de poste est gardé dans historiqueUtilisateurs.

## Demande d'un manager
- Un manager ne fait de demande que pour son département (celui de son poste).
- Il peut demander son propre remplacement, mais pas celui des autres managers ni de ses supérieurs
  (aucun poste tenu par un autre compte).
