-- Données de départ (développement) — à exécuter après base/init.sql

-- Statuts d'une demande : les id sont repris par l'enum gestion.hr.demande.Model.Statut (ne pas les changer)
INSERT INTO status (id, label) VALUES
    (1, 'EN_ATTENTE_HR'),
    (2, 'EN_ATTENTE_DIRECTION'),
    (3, 'EN_ATTENTE_FINANCE'),
    (4, 'APPROUVEE'),
    (5, 'REFUSEE');
SELECT setval('status_id_seq', 5);

-- Statuts d'un entretien : les id sont repris par l'enum Java du statut d'entretien (ne pas les changer)
INSERT INTO statusEntretien (id, label) VALUES
    (1, 'EN_ATTENTE'),
    (2, 'FINI');
SELECT setval('statusEntretien_id_seq', 2);

-- Référentiel de test
-- code : préfixe du code des postes (RH-2026-0001…)
INSERT INTO departement (code, label) VALUES
    ('RH',  'Ressources humaines'),
    ('INF', 'Informatique'),
    ('FIN', 'Finance'),
    ('DG',  'Direction générale');

INSERT INTO fonction (label) VALUES
    ('Chargé de recrutement'),
    ('Gestionnaire paie'),
    ('Développeur'),
    ('Administrateur système'),
    ('Comptable'),
    ('Contrôleur de gestion');

-- Postes existants (sièges), vacants (emploi.idPersonne NULL, plus bas)
-- code : <code du département>-<année>-<compteur>, un compteur par département et par année
INSERT INTO poste (code, label, idDepartement) VALUES
    ('RH-2026-0001',  'Chargé de recrutement', 1),
    ('RH-2026-0002',  'Gestionnaire paie', 1),
    ('INF-2026-0001', 'Développeur', 2),
    ('INF-2026-0002', 'Administrateur système', 2),
    ('FIN-2026-0001', 'Comptable', 3);

-- Postes d'importance tenus par les comptes de l'application (un compte = 1 poste, voir utilisateur)
INSERT INTO poste (code, label, idDepartement) VALUES
    ('INF-2026-0003', 'Manager informatique', 2),
    ('RH-2026-0003',  'Responsable RH', 1),
    ('DG-2026-0001',  'Directeur général', 4),
    ('FIN-2026-0002', 'Directeur financier', 3),
    ('INF-2026-0004', 'Administrateur de l''application', 2);

-- Détenteurs des postes des comptes (emploi.idPersonne) : affichés à chaque étape d'une demande
INSERT INTO personne (nom, prenom, naissance, adresse, tel, email) VALUES
    ('Rakotomalala',   'Hery',  '1985-03-14', 'Lot II M 45 Analakely, Antananarivo',    '034 12 345 67', 'hery.rakotomalala@exemple.mg'),
    ('Rasoanaivo',     'Lalao', '1979-07-02', 'Lot IVG 12 Ambohijatovo, Antananarivo',  '033 23 456 78', 'lalao.rasoanaivo@exemple.mg'),
    ('Andrianarivo',   'Fanja', '1968-11-23', 'Lot VA 8 Ivandry, Antananarivo',         '032 34 567 89', 'fanja.andrianarivo@exemple.mg'),
    ('Razafindrakoto', 'Mamy',  '1975-05-30', 'Lot III K 21 Ankorondrano, Antananarivo', '034 45 678 90', 'mamy.razafindrakoto@exemple.mg'),
    ('Rabearivelo',    'Toky',  '1990-01-09', 'Lot I B 3 Andravoahangy, Antananarivo',  '038 56 789 01', 'toky.rabearivelo@exemple.mg');

-- Un emploi par poste (poste = siège) : idPersonne NULL si le poste est vacant
INSERT INTO emploi (idPoste, idPersonne)
SELECT p.id, x.id
FROM poste p
LEFT JOIN (VALUES ('Manager informatique', 'Hery'), ('Responsable RH', 'Lalao'), ('Directeur général', 'Fanja'),
                  ('Directeur financier', 'Mamy'), ('Administrateur de l''application', 'Toky')) AS v (poste, prenom)
       ON v.poste = p.label
LEFT JOIN personne x ON x.prenom = v.prenom
ORDER BY p.id;

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
