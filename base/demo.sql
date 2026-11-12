-- Données de démonstration (développement) — à exécuter après base/init.sql et base/data.sql :
--   psql -U postgres -d gestion_hr -f base/demo.sql
-- Remplit chaque écran : demandes dans tous les statuts, avec l'historique des décisions de hr, direction, finance.
-- Dates relatives à aujourd'hui (CURRENT_DATE) : relancer le fichier sur une base neuve garde des délais crédibles.
-- Ne s'exécute qu'une fois (s'arrête si le compte manager.rh existe déjà).
-- Id repris de data.sql : départements 1 RH, 2 Informatique, 3 Finance ; fonctions 1-6 ; postes 1-2 (RH), 3-4 (Info), 5 (Finance)
-- (les postes 6 et plus sont ceux des comptes).

-- Deux managers de plus (mot de passe manager123, même hachage que manager) pour varier les demandeurs.
-- Un compte = 1 poste d'importance : on crée d'abord leur poste.
INSERT INTO poste (label, idDepartement)
SELECT label, dep FROM (VALUES ('Manager RH', 1), ('Manager finance', 3)) AS v (label, dep)
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE login = 'manager.rh');

-- Leurs détenteurs (emploi.idPersonne)
INSERT INTO personne (nom, prenom, naissance, adresse, tel, email)
SELECT nom, prenom, naissance::DATE, adresse, tel, email
FROM (VALUES
    ('Ravelojaona',   'Nirina', '1983-09-17', 'Lot II J 7 Isotry, Antananarivo',      '034 67 890 12', 'nirina.ravelojaona@exemple.mg'),
    ('Randrianasolo', 'Fidy',   '1987-04-05', 'Lot IVF 30 Ampefiloha, Antananarivo', '033 78 901 23', 'fidy.randrianasolo@exemple.mg')
) AS v (nom, prenom, naissance, adresse, tel, email)
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE login = 'manager.rh');

INSERT INTO emploi (idPoste, idPersonne)
SELECT p.id, x.id
FROM (VALUES ('Manager RH', 'Nirina'), ('Manager finance', 'Fidy')) AS v (poste, prenom)
JOIN poste p ON p.label = v.poste
JOIN personne x ON x.prenom = v.prenom
WHERE NOT EXISTS (SELECT 1 FROM emploi e WHERE e.idPoste = p.id);

INSERT INTO utilisateur (login, mdp, role, idPoste)
SELECT v.login, (SELECT mdp FROM utilisateur WHERE login = 'manager'), 'MANAGER',
       (SELECT max(id) FROM poste WHERE label = v.poste)
FROM (VALUES ('manager.rh', 'Manager RH'), ('manager.finance', 'Manager finance')) AS v (login, poste)
WHERE NOT EXISTS (SELECT 1 FROM utilisateur WHERE login = 'manager.rh');

INSERT INTO historiqueUtilisateurs (idUtilisateur, idPoste, status, date)
SELECT u.id, u.idPoste, u.status, u.creation FROM utilisateur u
WHERE u.login IN ('manager.rh', 'manager.finance')
  AND NOT EXISTS (SELECT 1 FROM historiqueUtilisateurs h WHERE h.idUtilisateur = u.id);

-- Crée une demande et rejoue ses décisions.
--   il_y_a    : création il y a N jours
--   delai     : délai en jours après la création (NULL = pas de délai)
--   creations : {{idFonction, effectif}, ...}   postes : {idPoste, ...}
--   decisions : une lettre par étape HR, Direction, Finance : V = validée, R = refusée ('' = en attente HR)
CREATE FUNCTION pg_temp.demo(demandeur TEXT, dep INT, il_y_a INT, motif TEXT, delai INT,
                             creations INT[], postes INT[], decisions TEXT) RETURNS VOID AS $$
DECLARE
    valideurs TEXT[] := ARRAY['hr', 'direction', 'finance'];
    jour DATE := CURRENT_DATE - il_y_a;
    statut INT := 1;
    id_demande INT;
    k INT;
BEGIN
    INSERT INTO demande (idDepartement, idStatus, idDemandeur, date, motif, delai)
    VALUES (dep, 1, (SELECT id FROM utilisateur WHERE login = demandeur), jour, motif, jour + delai)
    RETURNING id INTO id_demande;

    FOR k IN 1 .. COALESCE(array_length(creations, 1), 0) LOOP
        INSERT INTO demandeFonction (idFonction, idDemande, effectif) VALUES (creations[k][1], id_demande, creations[k][2]);
    END LOOP;
    FOR k IN 1 .. COALESCE(array_length(postes, 1), 0) LOOP
        INSERT INTO demandePoste (idPoste, idDemande) VALUES (postes[k], id_demande);
    END LOOP;

    -- Une décision tous les 1 à 3 jours, jamais après aujourd'hui
    FOR k IN 1 .. length(decisions) LOOP
        jour := LEAST(jour + 1 + (id_demande + k) % 3, CURRENT_DATE);
        statut := CASE substr(decisions, k, 1) WHEN 'V' THEN k + 1 ELSE 5 END;
        INSERT INTO histoDemande (idDemande, idUtilisateur, idStatus, date)
        VALUES (id_demande, (SELECT id FROM utilisateur WHERE login = valideurs[k]), statut, jour);
    END LOOP;
    UPDATE demande SET idStatus = statut WHERE id = id_demande;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM demande d JOIN utilisateur u ON u.id = d.idDemandeur WHERE u.login = 'manager.rh') THEN
        RAISE NOTICE 'Données de démo déjà présentes : rien à faire.';
        RETURN;
    END IF;

    -- En attente HR
    PERFORM pg_temp.demo('manager', 2, 1, 'Renfort de l''équipe de développement pour le nouveau portail client', 45,
        '{{3,2}}', '{}', '');
    PERFORM pg_temp.demo('manager.finance', 3, 3, 'Départ à la retraite du comptable principal', 21,
        '{}', '{5}', '');
    PERFORM pg_temp.demo('manager.rh', 1, 5, 'Hausse des recrutements prévue au premier trimestre', NULL,
        '{{1,1}}', '{}', '');
    PERFORM pg_temp.demo('manager', 2, 12, 'Remplacement urgent de l''administrateur système (démission)', 10,
        '{}', '{4}', '');   -- délai dépassé

    -- En attente Direction
    PERFORM pg_temp.demo('manager.rh', 1, 9, 'Gestionnaire paie en congé longue durée', 30,
        '{}', '{2}', 'V');
    PERFORM pg_temp.demo('manager', 2, 15, 'Migration des serveurs vers le cloud : besoin d''un administrateur en plus', 60,
        '{{4,1},{3,1}}', '{}', 'V');
    PERFORM pg_temp.demo('manager.finance', 3, 7, 'Mise en place du contrôle de gestion par projet', 40,
        '{{6,1}}', '{}', 'V');

    -- En attente Finance
    PERFORM pg_temp.demo('manager', 2, 20, 'Équipe mobile : deux développeurs et remplacement d''un poste vacant', 50,
        '{{3,2}}', '{3}', 'VV');
    PERFORM pg_temp.demo('manager.rh', 1, 14, 'Réorganisation du service recrutement', 35,
        '{{1,2}}', '{1}', 'VV');

    -- Approuvées
    PERFORM pg_temp.demo('manager.finance', 3, 40, 'Clôture annuelle : renfort comptable', 30,
        '{{5,2}}', '{}', 'VVV');
    PERFORM pg_temp.demo('manager', 2, 55, 'Remplacement du développeur parti en mutation', NULL,
        '{}', '{3}', 'VVV');
    PERFORM pg_temp.demo('manager.rh', 1, 30, 'Externalisation de la paie abandonnée : poste à pourvoir', 25,
        '{{2,1}}', '{}', 'VVV');

    -- Refusées (à chaque étape)
    PERFORM pg_temp.demo('manager', 2, 25, 'Création d''un poste de développeur stagiaire permanent', NULL,
        '{{3,5}}', '{}', 'R');
    PERFORM pg_temp.demo('manager.finance', 3, 35, 'Second contrôleur de gestion', 20,
        '{{6,1}}', '{}', 'VR');
    PERFORM pg_temp.demo('manager.rh', 1, 45, 'Chargé de recrutement supplémentaire pour la saison', 15,
        '{{1,1}}', '{}', 'VVR');
END;
$$;
