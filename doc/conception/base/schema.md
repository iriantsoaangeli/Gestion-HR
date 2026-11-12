# Schéma relationnel

## Organisation

**Un poste est comme un siege, son detenteur est dans emploi (idPersonne nulle si ce n'est pas occupe)**  
poste(**id**, label, #idDepartment)

**Le departement d'un poste**  
departement(**id**, label, nb)

**Historique de status d'un poste**    
histoPoste(**id**, #idPoste, status:true/false,datetime)

**Historique de creationd de poste , redondance avec histoposte**  
creationPoste(**id**,#idPoste,datetime)  

## Fonctions et qualifications

**Le travail qu'on donnera a un poste**  
fonction(**id**, label)

**Une fonction associee a un poste, le poste est comme un siege**  
fonctionPoste(**id**, #idPoste, #idFonction)

## Personnes et emplois

**Un individu dans la base de donnees**  
personne(**id**, nom, prenom, naissance,adresse,tel,email)

**Un  historique des changement d'un utilisateurs tout ce qui ne chang pas reste null**  
historiquePersonne(**id**,#idPersonne,nom?,prenom?,naissance?,adresse?,tel?,mail?,datetime)  

**On emploie une personne pour un poste sinon l'id est null**  
emploi(**id**, #idPoste, #idPersonne?)

**Les types de mouvements : entree/sortie**  
mouvement(**id**, label)

**Un historique des mouvement entree/sortie des employees**  
historiqueEmploi(**id**, datetime, #idMouvement, #idPersonne, #idPoste)  
> idPoste = poste dans lequel la personne entre ou dont elle sort

## Demandes de personnel

**Des status des demandes**  
status(**id**, label)

**Demande de personnel**  
demande(**id**, #idDepartement, #idStatus, #idDemandeur, datetime, motif, delai?)  
> idDemandeur = utilisateur (manager) qui a créé la demande


**Remplacement d'un poste existant** (1 poste = 1 siège, pas d'effectif)  
demandePoste(**id**, #idPoste, #idDemande)

**Création de postes** : le poste n'est pas encore créé, on désigne sa fonction et l'effectif voulu  
demandeFonction(**id**, #idFonction, #idDemande, effectif)

**Historique des décisions** (qui a validé / refusé, quand, statut obtenu)  
histoDemande(**id**, #idDemande, #idUtilisateur, #idStatus, datetime)  
> status : 1 EN_ATTENTE_HR, 2 EN_ATTENTE_DIRECTION, 3 EN_ATTENTE_FINANCE, 4 APPROUVEE, 5 REFUSEE (base/data.sql)


## Utilisateurs

**Les utilisateurs de l'application : un compte = 1 poste d'importance qui utilise l'application**  
utilisateur(**id**, login, mdp, role, #idPoste, status:true/false, creation)  
> mdp = hachage BCrypt ; role ∈ MANAGER, HR, DIRECTION, FINANCE, ADMIN (comptes de test : base/data.sql)  
> idPoste obligatoire et unique : chaque compte a un poste, un poste a au plus un compte  
> status = compte actif (true) ou désactivé (false) ; creation = datetime de création du compte

**Historique des comptes** (une ligne à la création, à chaque activation / désactivation ou changement de poste)  
historiqueUtilisateurs(**id**, #idUtilisateur, #idPoste, status:true/false, datetime)  
> idPoste et status = état du compte après le changement


## Creation d'offre d'emploi 

**Une offre d'emploi**  
offre(**id**,delai?,#idPoste,#idPosteResponsable,description,status:ouvert/clos,#idDemande,#idEmetteur)  

**Ceux qui vont faire l'entretien et les evalutations , du many to one**  
interviewer(**id**,#idPoste,#idOffre)

## Recrutement et entretien

**Le dossier de candidature** : une personne (employé ou externe) candidate à une offre, déposé par HR  
dossier(**id**,datetime,#idOffre,#idPersonne)  
> une personne dépose au plus un dossier par offre (UNIQUE idOffre, idPersonne)

**L'entretien d'un candidat** : l'offre et le candidat se retrouvent par le dossier  
entretien(**id**,datetime,#idDossier,#idStatusOffre)      

**L'evaluation d'un entretien**  
evaluation(**id**,#idEntretien,Note/100,Remarques  )  

**Le status de l'offre**
statusOffre(**id**,label)  