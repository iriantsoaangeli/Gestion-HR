# Schema relationnel :

## Tables :

**Un poste est comme un siege**  
poste(**id**,label)  

**Le departement d'un poste**  
departement(**id**,label)

**Table de liaison entre poste et departement**  
affectation(**id**,#idPoste,#idDepartement)  

personne(**id**,nom,prenom,naissance)  

**Le travail qu'on donnera a un poste**  
fonction(**id**,label)  

**Les types de mouvements : entree/sortie**  
mouvement(**id**,label)  

**Une fonction associee a un poste , le poste est comme un siege**  
fonctionPoste(**id**,#idPoste,#idFonction)  

**On emploie une personne pour un poste sinon l'id est null**  
emploi(**id**,#idPoste,#idPersonne?)  

**Un historique des mouvement entree/sortie des employees**  
historiqueEmploi(**id**,date,#idMouvement,#idPersonne)  
