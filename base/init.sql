-- Schéma relationnel (référence : doc/conception/base/schema.md)
-- SGBD : PostgreSQL
-- Les tables sont créées dans l'ordre des dépendances (clés étrangères).

-- =====================================================================
-- Tables de référence
-- =====================================================================

-- Un individu dans la base de donnees
CREATE TABLE personne (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    naissance DATE NOT NULL
);

-- Le departement d'un poste
CREATE TABLE departement (
    id SERIAL PRIMARY KEY,
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

-- Qualification d'un poste
CREATE TABLE clearance (
    id SERIAL PRIMARY KEY,
    label VARCHAR(100) NOT NULL
);

-- Des status des demandes
CREATE TABLE status (
    id SERIAL PRIMARY KEY,
    label VARCHAR(50) NOT NULL
);

-- =====================================================================
-- Organisation
-- =====================================================================

-- Un poste est comme un siege, idPersonne nulle si ce n'est pas occupe
-- (la redondance est voulue)
CREATE TABLE poste (
    id SERIAL PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    idPersonne INTEGER REFERENCES personne (id)
);

-- Table de liaison entre poste et departement
CREATE TABLE affectation (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idDepartement INTEGER NOT NULL REFERENCES departement (id)
);

-- Historique de status d'un poste
CREATE TABLE histoPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    date DATE NOT NULL DEFAULT NOW(),
    status BOOLEAN NOT NULL
);

-- Historique de creation de poste, redondance avec histoPoste
CREATE TABLE creationPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    date DATE NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Fonctions et qualifications
-- =====================================================================

-- Une fonction associee a un poste, le poste est comme un siege
CREATE TABLE fonctionPoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idFonction INTEGER NOT NULL REFERENCES fonction (id)
);

-- Table de liaison entre poste et clearance
CREATE TABLE clearancePoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idClearance INTEGER NOT NULL REFERENCES clearance (id)
);

-- =====================================================================
-- Personnes et emplois
-- =====================================================================

-- On emploie une personne pour un poste sinon l'id est null
CREATE TABLE emploi (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idPersonne INTEGER REFERENCES personne (id)
);

-- Historique d'affectation des personnes au postes
CREATE TABLE histoAffectation (
    id SERIAL PRIMARY KEY,
    idPersonne INTEGER NOT NULL REFERENCES personne (id),
    idMouvement INTEGER NOT NULL REFERENCES mouvement (id),
    date DATE NOT NULL
);

-- Un historique des mouvement entree/sortie des employees
CREATE TABLE historiqueEmploi (
    id SERIAL PRIMARY KEY,
    date DATE NOT NULL,
    idMouvement INTEGER NOT NULL REFERENCES mouvement (id),
    idPersonne INTEGER NOT NULL REFERENCES personne (id)
);

-- =====================================================================
-- Demandes de personnel
-- =====================================================================

-- Demande de personnel
CREATE TABLE demande (
    id SERIAL PRIMARY KEY,
    idDepartement INTEGER NOT NULL REFERENCES departement (id),
    idStatus INTEGER NOT NULL REFERENCES status (id),
    date DATE NOT NULL DEFAULT NOW(),
    motif VARCHAR(200) NOT NULL
);

-- Table de liaison entre demande et poste
CREATE TABLE demandePoste (
    id SERIAL PRIMARY KEY,
    idPoste INTEGER NOT NULL REFERENCES poste (id),
    idDemande INTEGER NOT NULL REFERENCES demande (id),
    effectif INTEGER NOT NULL CHECK (effectif > 0)
);