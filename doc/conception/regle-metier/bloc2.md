## Creation des postes 
Les postes vides sont cree avec l'offre 


## Remplacement 
Fonctionnalite future 

## Workflow 
- Depot de dossier : hr 
- Selection de dossier : hr
- Entretien : manager 
- Evalutation : manager et hr separement ou en meme temps  


## Entretien et evaluation 
Un interviewer redige une evaluation apres un entretien  
Le manager a la decision final  

## Recrutement  
Lorsque la decision finale est faite , l'offre est arretee  

## Précisions (décidées le 2026-11-12)

**Offre**
- Une offre vient d'une demande approuvée ; elle a un intitulé, une description, un profil recherché (facultatif)
  et une date limite (facultative). Pas de type de contrat.
- Une offre couvre un ou plusieurs postes (table offrePoste) :
  - remplacement : le poste existant de la demande ;
  - création (fonction × effectif) : le backend crée les postes vides à la publication de l'offre
    (un poste par effectif, emploi vacant), chacun avec un code lisible `<code du département>-<année>-<compteur>`,
    ex. `INF-2026-0007`, distinct de l'id.
- Statut de l'offre : ouverte (reçoit des dossiers) ou close.

**Dossier de candidature** (HR)
- Une personne de la base (employé ou externe) ou une nouvelle personne dépose au plus un dossier par offre ouverte.
- Pièces jointes : 30 Mo au total par dossier. Une pièce jointe appartient à la personne ; elle est rattachée
  au dossier par pieceJointeDossier (une même pièce peut servir à plusieurs dossiers de la personne).

**Entretien** (HR prépare)
- HR choisit une offre ouverte, un dossier de cette offre, la date et l'heure (dans le futur) et les
  interviewers (au moins un poste parmi ceux des comptes). Un candidat peut avoir plusieurs entretiens.
- Statut : En attente à la préparation, Fini quand tous les interviewers ont évalué (mis à jour par le backend).

**Évaluation**
- Chaque interviewer écrit sa propre évaluation de l'entretien : une seule par interviewer, note entière de 0 à 100
  et remarques obligatoires.
- On évalue seulement après la date et l'heure de l'entretien (décidé le 2026-11-13).
- Notes séparées : tant qu'un interviewer n'a pas rendu son évaluation, il ne voit ni les notes ni les remarques
  des autres (il voit seulement qui a évalué). Les autres rôles voient tout.
