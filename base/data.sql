-- Données de départ (développement) — à exécuter après base/init.sql

-- Statuts d'une demande : les id sont repris par l'enum gestion.hr.demande.Model.Statut (ne pas les changer)
INSERT INTO status (id, label) VALUES
    (1, 'EN_ATTENTE_HR'),
    (2, 'EN_ATTENTE_DIRECTION'),
    (3, 'EN_ATTENTE_FINANCE'),
    (4, 'APPROUVEE'),
    (5, 'REFUSEE');
SELECT setval('status_id_seq', 5);

-- Référentiel de test
INSERT INTO departement (label) VALUES
    ('Ressources humaines'),
    ('Informatique'),
    ('Finance'),
    ('Direction générale');

INSERT INTO fonction (label) VALUES
    ('Chargé de recrutement'),
    ('Gestionnaire paie'),
    ('Développeur'),
    ('Administrateur système'),
    ('Comptable'),
    ('Contrôleur de gestion');

-- Postes existants (sièges), vacants : idPersonne NULL
INSERT INTO poste (label, idDepartement) VALUES
    ('Chargé de recrutement', 1),
    ('Gestionnaire paie', 1),
    ('Développeur', 2),
    ('Administrateur système', 2),
    ('Comptable', 3);

-- Postes d'importance tenus par les comptes de l'application (un compte = 1 poste, voir utilisateur)
INSERT INTO poste (label, idDepartement) VALUES
    ('Manager informatique', 2),
    ('Responsable RH', 1),
    ('Directeur général', 4),
    ('Directeur financier', 3),
    ('Administrateur de l''application', 2);

-- Détenteurs des postes des comptes (poste.idPersonne) : affichés à chaque étape d'une demande
INSERT INTO personne (nom, prenom, naissance) VALUES
    ('Rakotomalala', 'Hery',  '1985-03-14'),
    ('Rasoanaivo',   'Lalao', '1979-07-02'),
    ('Andrianarivo', 'Fanja', '1968-11-23'),
    ('Razafindrakoto', 'Mamy', '1975-05-30'),
    ('Rabearivelo',  'Toky',  '1990-01-09');

UPDATE poste p SET idPersonne = x.id
FROM (VALUES ('Manager informatique', 'Hery'), ('Responsable RH', 'Lalao'), ('Directeur général', 'Fanja'),
             ('Directeur financier', 'Mamy'), ('Administrateur de l''application', 'Toky')) AS v (poste, prenom)
JOIN personne x ON x.prenom = v.prenom
WHERE p.label = v.poste;

-- Comptes de test, un par rôle. Mots de passe de DEV uniquement (à changer en production) :
--   manager / manager123   hr / hr123   direction / direction123   finance / finance123   admin / admin123
-- mdp = hachage BCrypt (Spring Security, BCryptPasswordEncoder), jamais le mot de passe en clair.
-- role : MANAGER, HR, DIRECTION, FINANCE, ADMIN (enum gestion.hr.auth.Model.Role)

-- idPoste : le poste d'importance du compte (créé juste au-dessus) ; status et creation par défaut (actif, aujourd'hui)

INSERT INTO utilisateur (login, mdp, role, idPoste)
SELECT v.login, v.mdp, v.role, p.id
FROM (VALUES
    ('manager',   '$2a$10$0uukbton6kPkDX7qA77yT.hzAYPbcwc5yzIzA/1Xl0m3E9V78LEo.', 'MANAGER',   'Manager informatique', 1),
    ('hr',        '$2a$10$P3w.N01qa/JA0IycPDqxD.oPXmbIPDi8EQ8g9PVL1WSsDaEKAoHWC', 'HR',        'Responsable RH', 2),
    ('direction', '$2a$10$5oTOCwDKbw0nYV5i7fnwc.1i.93EyX7i.9aRJFM0ji8X3xmJ4ZSYC', 'DIRECTION', 'Directeur général', 3),
    ('finance',   '$2a$10$3dmF2c.sRpF9fOZ0dbf9GOmQuzZB1ne.TzaufSm41Pz.g1xPxGvCO', 'FINANCE',   'Directeur financier', 4),
    ('admin',     '$2a$10$fO8Ag4rjlsOrwoRgeSShLO92kh7ucU6SzMg3gZ0tsVWeQ2gQQ70SW', 'ADMIN',     'Administrateur de l''application', 5)
) AS v (login, mdp, role, poste, ordre)
JOIN poste p ON p.label = v.poste
ORDER BY v.ordre; -- mêmes id qu'avant : manager 1, hr 2, direction 3, finance 4, admin 5

-- Création de chaque compte dans son historique
INSERT INTO historiqueUtilisateurs (idUtilisateur, idPoste, status, date)
SELECT id, idPoste, status, creation FROM utilisateur;
