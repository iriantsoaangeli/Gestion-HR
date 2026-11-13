-- Données de démonstration (développement) — à exécuter après base/init.sql et base/data.sql :
--   psql -U postgres -d gestion_hr -f base/demo.sql
-- Remplit chaque écran : demandes dans tous les statuts, avec l'historique des décisions de hr, direction, finance.
-- Dates relatives à aujourd'hui (CURRENT_DATE) : relancer le fichier sur une base neuve garde des délais crédibles.
-- Ne s'exécute qu'une fois (s'arrête si le compte manager.rh existe déjà).
-- Id repris de data.sql : départements 1 RH, 2 Informatique, 3 Finance ; fonctions 1-6 ; postes 1-2 (RH), 3-4 (Info), 5 (Finance)
-- (les postes 6 et plus sont ceux des comptes).

-- Deux managers de plus (mot de passe manager123, même hachage que manager) pour varier les demandeurs.
-- Un compte = 1 poste d'importance : on crée d'abord leur poste.
INSERT INTO poste (code, label, idDepartement)
SELECT code, label, dep FROM (VALUES ('RH-2026-0004', 'Manager RH', 1), ('FIN-2026-0003', 'Manager finance', 3)) AS v (code, label, dep)
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

-- =====================================================================
-- Recrutement (bloc 2) : offres, candidats, dossiers, pièces jointes, entretiens, évaluations
-- Repris des données statiques du front (api/offres.js, dossiers.js, entretiens.js).
-- Les offres partent des 3 demandes approuvées ci-dessus. Ne s'exécute qu'une fois (s'arrête si une offre existe).
-- =====================================================================

DO $$
DECLARE
    hr INT := (SELECT id FROM utilisateur WHERE login = 'hr');
    p_rh INT := (SELECT id FROM poste WHERE label = 'Responsable RH');
    p_info INT := (SELECT id FROM poste WHERE label = 'Manager informatique');
    p_fin INT := (SELECT max(id) FROM poste WHERE label = 'Manager finance');
    o_compta INT; o_dev INT; o_paie INT;
    p INT;
    d INT;
    e INT;
    pj INT;
    candidat RECORD;
BEGIN
    IF EXISTS (SELECT 1 FROM offre) THEN
        RAISE NOTICE 'Offres déjà présentes : rien à faire.';
        RETURN;
    END IF;

    -- Candidats externes (sans emploi)
    INSERT INTO personne (nom, prenom, naissance, adresse, tel, email) VALUES
        ('Rakotoarisoa',       'Andry',  '1996-02-11', 'Lot VB 41 Ambatobe, Antananarivo',     '034 11 222 33', 'andry.rakotoarisoa@exemple.mg'),
        ('Ramanantsoa',        'Volana', '1993-08-24', 'Lot III A 9 Ankadifotsy, Antananarivo', '033 22 333 44', 'volana.ramanantsoa@exemple.mg'),
        ('Rasolofonirina',     'Tiana',  '1998-12-03', 'Lot II F 17 Ambanidia, Antananarivo',   '032 33 444 55', 'tiana.rasolofonirina@exemple.mg'),
        ('Andriamanana',       'Haja',   '1989-06-15', 'Lot IVL 2 Ampasampito, Antananarivo',   '034 44 555 66', 'haja.andriamanana@exemple.mg'),
        ('Rahelimalala',       'Soa',    '1995-04-28', 'Lot I G 55 Behoririka, Antananarivo',   '038 55 666 77', 'soa.rahelimalala@exemple.mg'),
        ('Randriamampionona',  'Éric',   '1991-10-19', 'Lot VD 6 Ambohipo, Antananarivo',       '034 66 777 88', 'eric.randriamampionona@exemple.mg'),
        ('Rakotondrabe',       'Mialy',  '1997-01-07', 'Lot II Y 30 Mahamasina, Antananarivo',  '033 77 888 99', 'mialy.rakotondrabe@exemple.mg');

    -- Pièces jointes de chaque candidat (contenu texte de démonstration)
    FOR candidat IN SELECT id, prenom FROM personne
                    WHERE prenom IN ('Andry', 'Volana', 'Tiana', 'Haja', 'Soa', 'Éric', 'Mialy', 'Toky') LOOP
        INSERT INTO pieceJointe (idPersonne, nom, type, taille, contenu, date)
        VALUES (candidat.id, 'CV.txt', 'text/plain', octet_length('CV de ' || candidat.prenom),
                convert_to('CV de ' || candidat.prenom, 'UTF8'), CURRENT_DATE - 15);
    END LOOP;
    INSERT INTO pieceJointe (idPersonne, nom, type, taille, contenu, date)
    SELECT id, 'Lettre.txt', 'text/plain', octet_length('Lettre de motivation de ' || prenom),
           convert_to('Lettre de motivation de ' || prenom, 'UTF8'), CURRENT_DATE - 15
    FROM personne WHERE prenom IN ('Andry', 'Tiana', 'Éric');

    -- Offre 1 : création de 2 postes de comptable (2 postes vides créés avec l'offre)
    INSERT INTO offre (intitule, delai, idPosteResponsable, description, profil, status, idDemande, idEmetteur)
    VALUES ('Comptable', CURRENT_DATE + 20, p_fin,
            'Comptable confirmé(e) pour la clôture annuelle : saisie, rapprochements bancaires, déclarations fiscales.',
            'Bac+3 en comptabilité, 2 ans d''expérience, maîtrise d''un logiciel comptable.', 'OUVERT',
            (SELECT id FROM demande WHERE motif = 'Clôture annuelle : renfort comptable'), hr)
    RETURNING id INTO o_compta;
    FOR k IN 4 .. 5 LOOP
        INSERT INTO poste (code, label, idDepartement) VALUES ('FIN-2026-000' || k, 'Comptable', 3) RETURNING id INTO p;
        INSERT INTO fonctionPoste (idPoste, idFonction) VALUES (p, 5);
        INSERT INTO emploi (idPoste, idPersonne) VALUES (p, NULL);
        INSERT INTO creationPoste (idPoste, date) VALUES (p, CURRENT_DATE - 10);
        INSERT INTO offrePoste (idOffre, idPoste) VALUES (o_compta, p);
    END LOOP;

    -- Offre 2 : remplacement du poste de développeur existant
    INSERT INTO offre (intitule, delai, idPosteResponsable, description, profil, status, idDemande, idEmetteur)
    VALUES ('Développeur', NULL, p_info,
            'Développeur(se) web Vue.js / Spring Boot pour remplacer un départ en mutation.', NULL, 'OUVERT',
            (SELECT id FROM demande WHERE motif = 'Remplacement du développeur parti en mutation'), hr)
    RETURNING id INTO o_dev;
    INSERT INTO offrePoste (idOffre, idPoste) VALUES (o_dev, (SELECT id FROM poste WHERE code = 'INF-2026-0001'));

    -- Offre 3 : création d'un poste de gestionnaire paie, close (entretiens finis)
    INSERT INTO offre (intitule, delai, idPosteResponsable, description, profil, status, idDemande, idEmetteur)
    VALUES ('Gestionnaire paie', CURRENT_DATE - 5, p_rh,
            'Reprise de la paie en interne : bulletins, déclarations sociales, suivi des absences.', NULL, 'CLOS',
            (SELECT id FROM demande WHERE motif = 'Externalisation de la paie abandonnée : poste à pourvoir'), hr)
    RETURNING id INTO o_paie;
    INSERT INTO poste (code, label, idDepartement) VALUES ('RH-2026-0005', 'Gestionnaire paie', 1) RETURNING id INTO p;
    INSERT INTO fonctionPoste (idPoste, idFonction) VALUES (p, 2);
    INSERT INTO emploi (idPoste, idPersonne) VALUES (p, NULL);
    INSERT INTO creationPoste (idPoste, date) VALUES (p, CURRENT_DATE - 25);
    INSERT INTO offrePoste (idOffre, idPoste) VALUES (o_paie, p);

    -- Dossiers (avec toutes les pièces jointes du candidat), entretiens, interviewers, évaluations.
    -- jours : date de l'entretien par rapport à aujourd'hui ; notes : {{idPoste, note}} déjà rendues
    CREATE TEMP TABLE demo_entretien (offre INT, prenom TEXT, jours INT, heure TIME, interviewers INT[], notes INT[][], remarques TEXT[])
        ON COMMIT DROP;
    INSERT INTO demo_entretien VALUES
        (o_compta, 'Andry',  6, '09:00', ARRAY[p_rh], NULL, NULL),
        (o_compta, 'Volana', 6, '14:30', ARRAY[p_rh], NULL, NULL),
        (o_compta, 'Tiana',  NULL, NULL, NULL, NULL, NULL),
        (o_dev,    'Tiana',  8, '10:00', ARRAY[p_info, p_rh], NULL, NULL),
        (o_dev,    'Haja',  -2, '08:30', ARRAY[p_info, p_rh], ARRAY[[p_rh, 70]], ARRAY['Bon contact, connaît bien Linux.']),
        (o_dev,    'Andry', -3, '14:00', ARRAY[p_info, p_rh], ARRAY[[p_info, 85], [p_rh, 80]],
                   ARRAY['Très bon test technique (Vue.js et Spring Boot).', 'Motivé, disponible tout de suite.']),
        (o_dev,    'Toky',  NULL, NULL, NULL, NULL, NULL),
        (o_paie,   'Éric',  -7, '09:30', ARRAY[p_rh], ARRAY[[p_rh, 78]],
                   ARRAY['Bonne maîtrise de la paie, à l''aise avec les déclarations sociales.']),
        (o_paie,   'Mialy', -6, '11:00', ARRAY[p_rh], ARRAY[[p_rh, 64]],
                   ARRAY['Motivée, mais peu d''expérience sur un logiciel de paie.']),
        (o_dev,    'Soa',   NULL, NULL, NULL, NULL, NULL);

    FOR candidat IN SELECT x.*, pe.id AS idPersonne FROM demo_entretien x JOIN personne pe ON pe.prenom = x.prenom LOOP
        SELECT id INTO d FROM dossier WHERE idOffre = candidat.offre AND idPersonne = candidat.idPersonne;
        IF d IS NULL THEN
            INSERT INTO dossier (date, idOffre, idPersonne)
            VALUES (CURRENT_DATE - 12, candidat.offre, candidat.idPersonne) RETURNING id INTO d;
            INSERT INTO pieceJointeDossier (idPieceJointe, idDossier)
            SELECT id, d FROM pieceJointe WHERE idPersonne = candidat.idPersonne;
        END IF;
        CONTINUE WHEN candidat.jours IS NULL;  -- dossier sans entretien

        INSERT INTO entretien (date, idDossier, idStatusEntretien)
        VALUES ((CURRENT_DATE + candidat.jours) + candidat.heure, d,
                CASE WHEN COALESCE(array_length(candidat.notes, 1), 0) = array_length(candidat.interviewers, 1)
                     THEN 2 ELSE 1 END)
        RETURNING id INTO e;
        INSERT INTO interviewer (idPoste, idEntretien) SELECT unnest(candidat.interviewers), e;
        FOR k IN 1 .. COALESCE(array_length(candidat.notes, 1), 0) LOOP
            INSERT INTO evaluation (idEntretien, idPoste, note, remarques, date)
            VALUES (e, candidat.notes[k][1], candidat.notes[k][2], candidat.remarques[k],
                    (CURRENT_DATE + candidat.jours) + candidat.heure + INTERVAL '2 hours');
        END LOOP;
    END LOOP;
END;
$$;
