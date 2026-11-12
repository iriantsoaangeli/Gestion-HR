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
demande(**id**, #idDepartement, #idStatus, #idDemandeur, date, motif, delai?)  
> idDemandeur = utilisateur (manager) qui a créé la demande


**Remplacement d'un poste existant** (1 poste = 1 siège, pas d'effectif)  
demandePoste(**id**, #idPoste, #idDemande)

**Création de postes** : le poste n'est pas encore créé, on désigne sa fonction et l'effectif voulu  
demandeFonction(**id**, #idFonction, #idDemande, effectif)

**Historique des décisions** (qui a validé / refusé, quand, statut obtenu)  
histoDemande(**id**, #idDemande, #idUtilisateur, #idStatus, date)  
> status : 1 EN_ATTENTE_HR, 2 EN_ATTENTE_DIRECTION, 3 EN_ATTENTE_FINANCE, 4 APPROUVEE, 5 REFUSEE (base/data.sql)


## Utilisateurs

**Les utilisateurs de l'application : un compte = 1 poste d'importance qui utilise l'application**  
utilisateur(**id**, login, mdp, role, #idPoste, status:true/false, creation)  
> mdp = hachage BCrypt ; role ∈ MANAGER, HR, DIRECTION, FINANCE, ADMIN (comptes de test : base/data.sql)  
> idPoste obligatoire et unique : chaque compte a un poste, un poste a au plus un compte  
> status = compte actif (true) ou désactivé (false) ; creation = date de création du compte

**Historique des comptes** (une ligne à la création, à chaque activation / désactivation ou changement de poste)  
historiqueUtilisateurs(**id**, #idUtilisateur, #idPoste, status:true/false, date)  
> idPoste et status = état du compte après le changement


## Creation d'offre d'emploi 

**Une offre d'emploi**  
offre(**id**,delai?,#idPoste,#idPosteResponsable,description)  

**Ceux qui vont faire l'entretien et les evalutations , du many to one**  
interviewer(**id**,#idPoste,#idOffre)