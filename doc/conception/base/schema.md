# Schéma relationnel

## Organisation

**Un poste est comme un siege, idPersonne nulle si ce n'est pas occupe (la redondance est voulue)**  
poste(**id**, label, #idPersonne?,#idDepartment)

**Le departement d'un poste**  
departement(**id**, label, nb)

**Historique de status d'un poste**    
histoPoste(**id**, #idPoste, status:true/false,date)

**Historique de creationd de poste , redondance avec histoposte**  
creationPoste(**id**,#idPoste,date)  

## Fonctions et qualifications

**Le travail qu'on donnera a un poste**  
fonction(**id**, label)

**Une fonction associee a un poste, le poste est comme un siege**  
fonctionPoste(**id**, #idPoste, #idFonction)

**Qualification d'un poste**  
clearance(**id**, label)

**Table de liaison**  
clearancePoste(**id**, #idPoste,#idClearance)

## Personnes et emplois

**Un individu dans la base de donnees**  
personne(**id**, nom, prenom, naissance)

**On emploie une personne pour un poste sinon l'id est null**  
emploi(**id**, #idPoste, #idPersonne?)

**Les types de mouvements : entree/sortie**  
mouvement(**id**, label)

**Historique d'affectation des personnes au postes**  
histoAffectation(**id**, #idPersonne, #idMouvement, date)

**Un historique des mouvement entree/sortie des employees**  
historiqueEmploi(**id**, date, #idMouvement, #idPersonne)

## Demandes de personnel

**Des status des demandes**  
status(**id**, label)

**Demande de personnel**  
demande(**id**, #idDepartement, #idStatus,date,motif)


**Table de liason**
demandePoste(**id**,#idPoste,#idDemande)