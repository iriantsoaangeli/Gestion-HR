-- Données de départ (développement) — à exécuter après base/init.sql
-- Comptes de test, un par rôle. Mots de passe de DEV uniquement (à changer en production) :
--   manager / manager123   hr / hr123   direction / direction123   finance / finance123   admin / admin123
-- mdp = hachage BCrypt (Spring Security, BCryptPasswordEncoder), jamais le mot de passe en clair.
-- role : MANAGER, HR, DIRECTION, FINANCE, ADMIN (enum gestion.hr.auth.Role)

INSERT INTO utilisateur (login, mdp, role) VALUES
    ('manager',   '$2a$10$0uukbton6kPkDX7qA77yT.hzAYPbcwc5yzIzA/1Xl0m3E9V78LEo.', 'MANAGER'),
    ('hr',        '$2a$10$P3w.N01qa/JA0IycPDqxD.oPXmbIPDi8EQ8g9PVL1WSsDaEKAoHWC', 'HR'),
    ('direction', '$2a$10$5oTOCwDKbw0nYV5i7fnwc.1i.93EyX7i.9aRJFM0ji8X3xmJ4ZSYC', 'DIRECTION'),
    ('finance',   '$2a$10$3dmF2c.sRpF9fOZ0dbf9GOmQuzZB1ne.TzaufSm41Pz.g1xPxGvCO', 'FINANCE'),
    ('admin',     '$2a$10$fO8Ag4rjlsOrwoRgeSShLO92kh7ucU6SzMg3gZ0tsVWeQ2gQQ70SW', 'ADMIN');
