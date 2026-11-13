# Schéma relationnel

## Organisation

**Un poste est comme un siege, son detenteur est dans emploi (idPersonne nulle si ce n'est pas occupe)**  
poste(**id**, code, label, #idDepartment)  
> code : identifiant lisible, unique, distinct de l'id : `<code du département>-<année>-<compteur>` (ex. INF-2026-0007)

**Le departement d'un poste**  
departement(**id**, code, label, nb)  
> code : court et unique (RH, INF, FIN, DG), préfixe du code des postes

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

**Une offre d'emploi**, issue d'une demande approuvée ; ses postes sont dans offrePoste  
offre(**id**,intitule,delai?,#idPosteResponsable,description,profil?,status:ouvert/clos,#idDemande,#idEmetteur)  

**Les postes couverts par une offre** : remplacement = le poste existant ; création = postes vides créés par le backend
à la publication (un par effectif)  
offrePoste(**id**,#idOffre,#idPoste)  
> UNIQUE (idOffre, idPoste)

## Recrutement et entretien

**Le dossier de candidature** : une personne (employé ou externe) candidate à une offre, déposé par HR  
dossier(**id**,datetime,#idOffre,#idPersonne)  
> une personne dépose au plus un dossier par offre (UNIQUE idOffre, idPersonne)

**Une pièce jointe** appartient à une personne (CV, lettre…) ; contenu stocké en base  
pieceJointe(**id**,#idPersonne,nom,type,taille,contenu,datetime)  

**Les pièces jointes d'un dossier** (30 Mo au total par dossier)  
pieceJointeDossier(**id**,#idPieceJointe,#idDossier)  
> UNIQUE (idPieceJointe, idDossier)

**Le statut d'un entretien**  
statusEntretien(**id**,label)  
> 1 EN_ATTENTE, 2 FINI (base/data.sql, repris par un enum Java : ne pas changer les id)

**L'entretien d'un candidat** : l'offre et le candidat se retrouvent par le dossier  
entretien(**id**,datetime,#idDossier,#idStatusEntretien)  

**Ceux qui font l'entretien et l'évaluent** (postes des comptes)  
interviewer(**id**,#idPoste,#idEntretien)  
> UNIQUE (idEntretien, idPoste)

**L'évaluation d'un entretien** : une par interviewer  
evaluation(**id**,#idEntretien,#idPoste,note/100,remarques,datetime)  
> UNIQUE (idEntretien, idPoste) ; idPoste = l'interviewer qui l'a écrite
