-- Schéma relationnel (référence : doc/conception/base/schema.md)
-- SGBD : PostgreSQL
-- Les tables sont créées dans l'ordre des dépendances (clés étrangères).
-- « datetime » du schéma = colonne date de type TIMESTAMP (date + heure) ; delai et naissance restent des DATE.

-- =====================================================================
-- Tables de référence
-- =====================================================================

-- Un individu dans la base de donnees
CREATE TABLE personne (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    naissance DATE NOT NULL,
    adresse VARCHAR(200) NOT NULL,
    tel VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL
);

-- Le departement d'un poste ; code court et unique (RH, INF…), prefixe du code des postes
CREATE TABLE departement (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    nb INTEGER NOT NULL DEFAULT 0
);

-- Le travail qu'on donnera a un poste
CREATE TABLE fonction (
    id SERIAL PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);

-- Les types de mouvements : entree/sortie
CREATE TABLE mouvement (
    id SERIAL PRIMARY KEY,
    label VARCHAR(50) NOT NULL
);

-- Des status des demandes
CREATE TABLE status (
    id SERIAL PRIMARY KEY,
    label VARCHAR(50) NOT NULL
);

-- Le statut d'un entretien : 1 EN_ATTENTE, 2 FINI (id repris par l'enum Java, base/data.sql)
CREATE TABLE statusEntretien (
    id SERIAL PRIMARY KEY,
    label VARCHAR(50) NOT NULL
);

-- =====================================================================
-- Organisation
-- =====================================================================

-- Un poste est comme un siege ; son detenteur est dans emploi
-- code : identifiant lisible et unique, distinct de l'id : <code du departement>-<annee>-<compteur> (INF-2026-0007)
CREATE TABLE poste (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    idDepartement INTEGER NOT NULL REFERENCES departement (id)
);

-- Historique de status d'un poste
CREATE TABLE histoPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    status BOOLEAN NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Historique de creation de poste, redondance avec histoPoste
CREATE TABLE creationPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Fonctions
-- =====================================================================

-- Une fonction associee a un poste, le poste est comme un siege
CREATE TABLE fonctionPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idFonction INTEGER NOT NULL REFERENCES fonction (id)
);

-- =====================================================================
-- Personnes et emplois
-- =====================================================================

-- Historique des changements d'une personne : une ligne par changement,
-- ce qui ne change pas reste null
CREATE TABLE historiquePersonne (
    id SERIAL PRIMARY KEY,
    idPersonne INTEGER NOT NULL REFERENCES personne (id),
    nom VARCHAR(100) DEFAULT NULL,
    prenom VARCHAR(100) DEFAULT NULL,
    naissance DATE DEFAULT NULL,
    adresse VARCHAR(200) DEFAULT NULL,
    tel VARCHAR(20) DEFAULT NULL,
    mail VARCHAR(100) DEFAULT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- On emploie une personne pour un poste sinon l'id est null (poste vacant)
-- Un poste = un siege : une seule ligne par poste (UNIQUE)
CREATE TABLE emploi (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL UNIQUE REFERENCES poste (id),
    idPersonne INTEGER REFERENCES personne (id)
);

-- Un historique des mouvement entree/sortie des employees
-- idPoste = poste dans lequel la personne entre ou dont elle sort
CREATE TABLE historiqueEmploi (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL DEFAULT NOW(),
    idMouvement INTEGER NOT NULL REFERENCES mouvement (id),
    idPersonne INTEGER NOT NULL REFERENCES personne (id),
    idPoste INTEGER NOT NULL REFERENCES poste (id)
);

-- =====================================================================
-- Utilisateurs
-- =====================================================================

-- Utilisateurs de l'application (mdp = hachage BCrypt, role = enum gestion.hr.auth.Model.Role)
-- Un compte = 1 poste d'importance : idPoste obligatoire, et un poste a au plus un compte (UNIQUE)
-- status : compte actif (true) ou desactive (false) ; creation : date et heure de creation du compte
CREATE TABLE utilisateur (
    id SERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    mdp VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    idPoste INTEGER NOT NULL UNIQUE REFERENCES poste (id),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    creation TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Historique d'un compte : creation, activation / desactivation, changement de poste
-- idPoste et status = etat du compte apres le changement
CREATE TABLE historiqueUtilisateurs (
    id SERIAL PRIMARY KEY,
    idUtilisateur INTEGER NOT NULL REFERENCES utilisateur (id),
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    status BOOLEAN NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Demandes de personnel
-- =====================================================================

-- Demande de personnel, creee par un manager (idDemandeur)
CREATE TABLE demande (
    id SERIAL PRIMARY KEY,
    idDepartement INTEGER NOT NULL REFERENCES departement (id),
    idStatus INTEGER NOT NULL REFERENCES status (id),
    idDemandeur INTEGER NOT NULL REFERENCES utilisateur (id),
    date TIMESTAMP NOT NULL DEFAULT NOW(),
    motif VARCHAR(200) NOT NULL,
    delai DATE DEFAULT NULL
);

-- Remplacement : un poste existant a remplir (1 poste = 1 siege, pas d'effectif)
CREATE TABLE demandePoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idDemande INTEGER NOT NULL REFERENCES demande (id)
);

-- Creation : le poste n'est pas encore cree (en suspens), on designe sa fonction
-- et le nombre de postes voulus (l'effectif n'existe que pour une creation)
CREATE TABLE demandeFonction (
    id SERIAL PRIMARY KEY,
    idFonction INTEGER NOT NULL REFERENCES fonction (id),
    idDemande INTEGER NOT NULL REFERENCES demande (id),
    effectif INTEGER NOT NULL CHECK (effectif > 0)
);

-- Historique des decisions sur une demande : qui a valide / refuse, quand, et le statut obtenu
CREATE TABLE histoDemande (
    id SERIAL PRIMARY KEY,
    idDemande INTEGER NOT NULL REFERENCES demande (id),
    idUtilisateur INTEGER NOT NULL REFERENCES utilisateur (id),
    idStatus INTEGER NOT NULL REFERENCES status (id),
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Offres d'emploi
-- =====================================================================

-- Offre d'emploi issue d'une demande approuvee (idDemande) ; ses postes sont dans offrePoste
-- idPosteResponsable = poste qui suit le recrutement ; idEmetteur = compte qui a publie l'offre
-- delai : date limite de candidature (facultative) ; profil : profil recherche (facultatif)
-- status : offre ouverte ou close
CREATE TABLE offre (
    id SERIAL PRIMARY KEY,
    intitule VARCHAR(150) NOT NULL,
    delai DATE DEFAULT NULL,
    idPosteResponsable INTEGER NOT NULL REFERENCES poste (id),
    description TEXT NOT NULL,
    profil TEXT DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OUVERT' CHECK (status IN ('OUVERT', 'CLOS')),
    idDemande INTEGER NOT NULL REFERENCES demande (id),
    idEmetteur INTEGER NOT NULL REFERENCES utilisateur (id)
);

-- Les postes couverts par une offre : remplacement = le poste existant ;
-- creation = postes vides crees par le backend a la publication (un par effectif)
CREATE TABLE offrePoste (
    id SERIAL PRIMARY KEY,
    idOffre INTEGER NOT NULL REFERENCES offre (id),
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    UNIQUE (idOffre, idPoste)
);

-- =====================================================================
-- Recrutement et entretien
-- =====================================================================

-- Dossier de candidature : une personne (employe ou externe) candidate a une offre, depose par HR
-- Une personne depose au plus un dossier par offre
CREATE TABLE dossier (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL DEFAULT NOW(),
    idOffre INTEGER NOT NULL REFERENCES offre (id),
    idPersonne INTEGER NOT NULL REFERENCES personne (id),
    UNIQUE (idOffre, idPersonne)
);

-- Piece jointe d'une personne (CV, lettre…) ; contenu stocke en base ; taille en octets
CREATE TABLE pieceJointe (
    id SERIAL PRIMARY KEY,
    idPersonne INTEGER NOT NULL REFERENCES personne (id),
    nom VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    taille INTEGER NOT NULL CHECK (taille >= 0),
    contenu BYTEA NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Les pieces jointes d'un dossier (30 Mo au total par dossier, controle par le backend)
CREATE TABLE pieceJointeDossier (
    id SERIAL PRIMARY KEY,
    idPieceJointe INTEGER NOT NULL REFERENCES pieceJointe (id),
    idDossier INTEGER NOT NULL REFERENCES dossier (id),
    UNIQUE (idPieceJointe, idDossier)
);

-- Entretien d'un candidat : l'offre et le candidat se retrouvent par le dossier
-- idStatusEntretien : EN_ATTENTE a la preparation, FINI quand tous les interviewers ont evalue
CREATE TABLE entretien (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL,
    idDossier INTEGER NOT NULL REFERENCES dossier (id),
    idStatusEntretien INTEGER NOT NULL REFERENCES statusEntretien (id)
);

-- Ceux qui font l'entretien et l'evaluent (postes des comptes)
CREATE TABLE interviewer (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idEntretien INTEGER NOT NULL REFERENCES entretien (id),
    UNIQUE (idEntretien, idPoste)
);

-- Evaluation d'un entretien : une par interviewer (idPoste), note sur 100 et remarques
CREATE TABLE evaluation (
    id SERIAL PRIMARY KEY,
    idEntretien INTEGER NOT NULL REFERENCES entretien (id),
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    note INTEGER NOT NULL CHECK (note BETWEEN 0 AND 100),
    remarques TEXT NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (idEntretien, idPoste)
);
