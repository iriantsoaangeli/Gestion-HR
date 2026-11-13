package gestion.hr.dossier.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.commun.Validation.Texte;
import gestion.hr.dossier.Dto.DeposerDossierRequest;
import gestion.hr.dossier.Dto.DossierResponse;
import gestion.hr.dossier.Model.Dossier;
import gestion.hr.dossier.Model.PieceJointe;
import gestion.hr.dossier.Model.PieceJointeDossier;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.dossier.Repository.PieceJointeDossierRepository;
import gestion.hr.dossier.Repository.PieceJointeRepository;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Repository.OffreRepository;
import gestion.hr.referentiel.Model.Personne;
import gestion.hr.referentiel.Repository.PersonneRepository;

/**
 * Dépôt d'un dossier de candidature (appelé seulement par DossierService, qui porte la transaction : si une étape
 * échoue, rien n'est enregistré, ni la nouvelle personne ni les fichiers).
 * - HR seulement ; l'offre doit être ouverte.
 * - Candidat : une personne de la base, ou une nouvelle personne (tous les champs de la table personne), refusée si
 *   elle existe déjà (même e-mail, ou même nom + prénom + naissance, sans casse ni accents : comme le front).
 * - Une personne dépose au plus un dossier par offre (409 DEJA_FAIT).
 * - Au moins un fichier, 30 Mo au total. Chaque fichier devient une pieceJointe de la personne, reliée au dossier
 *   par pieceJointeDossier.
 * Tout est vérifié avant d'écrire quoi que ce soit.
 */
@Component
class DepotDossier {

    /** 30 Mo pour toutes les pièces jointes d'un dossier (le multipart de Spring s'arrête un peu au-dessus). */
    static final long TAILLE_MAX = 30L * 1024 * 1024;
    static final int NOM_FICHIER_MAX = 255;
    static final int TYPE_MAX = 100;
    static final LocalDate NAISSANCE_MIN = LocalDate.of(1900, 1, 1);

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TEL = Pattern.compile("^\\+?[0-9 .()-]{6,20}$");

    private final AccesDossier acces;
    private final OffreRepository offres;
    private final DossierRepository dossiers;
    private final PersonneRepository personnes;
    private final PieceJointeRepository pieces;
    private final PieceJointeDossierRepository liens;
    private final LectureDossier lecture;

    DepotDossier(AccesDossier acces, OffreRepository offres, DossierRepository dossiers, PersonneRepository personnes,
            PieceJointeRepository pieces, PieceJointeDossierRepository liens, LectureDossier lecture) {
        this.acces = acces;
        this.offres = offres;
        this.dossiers = dossiers;
        this.personnes = personnes;
        this.pieces = pieces;
        this.liens = liens;
        this.lecture = lecture;
    }

    DossierResponse deposer(DeposerDossierRequest req, List<MultipartFile> parts) {
        acces.exigerHr();
        Offre offre = offreOuverte(req.idOffre());
        Personne existante = null;
        Personne nouvelle = null;
        if (estNouvelle(req)) {
            if (!Texte.vide(req.idPersonne())) {
                throw ErreurMetier.validation(
                        "Choisissez une personne de la base ou une nouvelle personne, pas les deux.");
            }
            nouvelle = nouvellePersonne(req);
        } else {
            existante = personneExistante(req.idPersonne(), offre);
        }
        List<Fichier> fichiers = fichiers(parts);

        Personne candidat = existante != null ? existante : personnes.save(nouvelle);
        Dossier dossier = dossiers.save(new Dossier(offre.getId(), candidat.getId()));
        for (Fichier f : fichiers) {
            PieceJointe pj = pieces.save(new PieceJointe(candidat.getId(), f.nom(), f.type(), f.contenu()));
            liens.save(new PieceJointeDossier(pj.getId(), dossier.getId()));
        }
        return lecture.enReponse(dossier);
    }

    // ---------- Offre ----------

    private Offre offreOuverte(String idOffre) {
        if (Texte.vide(idOffre)) {
            throw ErreurMetier.validation("Choisissez une offre.");
        }
        Offre o = offres.findById(entier(idOffre, "Offre inconnue.")).orElse(null);
        if (o == null) {
            throw ErreurMetier.validation("Offre inconnue.");
        }
        if (!o.estOuverte()) {
            throw ErreurMetier.validation("L'offre « " + o.getIntitule() + " » est close : elle ne reçoit plus de dossiers.");
        }
        return o;
    }

    // ---------- Candidat ----------

    private Personne personneExistante(String idPersonne, Offre offre) {
        if (Texte.vide(idPersonne)) {
            throw ErreurMetier.validation("Choisissez un candidat dans la base, ou ajoutez une nouvelle personne.");
        }
        Personne p = personnes.findById(entier(idPersonne, "Personne inconnue.")).orElse(null);
        if (p == null) {
            throw ErreurMetier.validation("Personne inconnue.");
        }
        if (dossiers.findByIdOffreAndIdPersonne(offre.getId(), p.getId()).isPresent()) {
            throw ErreurMetier.dejaFait(p.getNomComplet() + " a déjà déposé un dossier pour cette offre.");
        }
        return p;
    }

    /** Vrai si au moins un champ de nouvelle personne est rempli. */
    private static boolean estNouvelle(DeposerDossierRequest r) {
        return !Texte.vide(r.nom()) || !Texte.vide(r.prenom()) || !Texte.vide(r.naissance())
                || !Texte.vide(r.adresse()) || !Texte.vide(r.tel()) || !Texte.vide(r.email());
    }

    /** Vérifie les champs (table personne : tous obligatoires) et les doublons ; ne l'enregistre pas. */
    private Personne nouvellePersonne(DeposerDossierRequest r) {
        String nom = Texte.obligatoire(r.nom(), "Le nom", 100);
        String prenom = Texte.obligatoire(r.prenom(), "Le prénom", 100);
        LocalDate naissance = naissance(r.naissance());
        String adresse = Texte.obligatoire(r.adresse(), "L'adresse", 200);
        String tel = Texte.obligatoire(r.tel(), "Le téléphone", 20);
        String email = Texte.obligatoire(r.email(), "L'e-mail", 100);
        if (!TEL.matcher(tel).matches()) {
            throw ErreurMetier.validation("Le téléphone n'est pas valide (chiffres, espaces, +, -, . et parenthèses).");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw ErreurMetier.validation("L'e-mail n'est pas valide.");
        }

        List<Personne> memes = new ArrayList<>(personnes.avecEmail(email));
        personnes.findByNaissance(naissance).stream()
                .filter(p -> sansAccent(p.getNom()).equals(sansAccent(nom))
                        && sansAccent(p.getPrenom()).equals(sansAccent(prenom)))
                .forEach(memes::add);
        if (!memes.isEmpty()) {
            Personne p = memes.get(0);
            throw ErreurMetier.validation("Cette personne existe déjà (" + p.getNomComplet() + ", " + p.getEmail()
                    + ") : sélectionnez-la au lieu de la recréer.");
        }
        return new Personne(nom, prenom, naissance, adresse, tel, email);
    }

    private static LocalDate naissance(String texte) {
        if (Texte.vide(texte)) {
            throw ErreurMetier.validation("La date de naissance est obligatoire.");
        }
        LocalDate d;
        try {
            d = LocalDate.parse(texte.trim());
        } catch (DateTimeParseException e) {
            throw ErreurMetier.validation("La date de naissance n'est pas valide (format AAAA-MM-JJ).");
        }
        if (!d.isBefore(LocalDate.now())) {
            throw ErreurMetier.validation("La date de naissance doit être dans le passé.");
        }
        if (d.isBefore(NAISSANCE_MIN)) {
            throw ErreurMetier.validation("La date de naissance est trop ancienne.");
        }
        return d;
    }

    // ---------- Fichiers ----------

    /** Fichier reçu, vérifié et lu. */
    private record Fichier(String nom, String type, byte[] contenu) {
    }

    private List<Fichier> fichiers(List<MultipartFile> parts) {
        List<MultipartFile> recus = new ArrayList<>();
        for (MultipartFile f : parts == null ? List.<MultipartFile>of() : parts) {
            // Un champ fichier laissé vide arrive comme une part sans nom et sans contenu : on l'ignore
            if (f != null && !(f.isEmpty() && Texte.vide(f.getOriginalFilename()))) {
                recus.add(f);
            }
        }
        if (recus.isEmpty()) {
            throw ErreurMetier.validation("Joignez au moins un fichier (CV, lettre de motivation…).");
        }

        long total = recus.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > TAILLE_MAX) {
            throw ErreurMetier.validation("Les pièces jointes dépassent 30 Mo au total (" + enMo(total) + ").");
        }

        Set<String> noms = new HashSet<>();
        List<Fichier> resultat = new ArrayList<>();
        for (MultipartFile f : recus) {
            String nom = nomFichier(f.getOriginalFilename());
            if (f.isEmpty()) {
                throw ErreurMetier.validation("Le fichier « " + nom + " » est vide.");
            }
            if (!noms.add(nom.toLowerCase(Locale.ROOT))) {
                throw ErreurMetier.validation("Le fichier « " + nom + " » est joint deux fois.");
            }
            try {
                resultat.add(new Fichier(nom, type(f.getContentType()), f.getBytes()));
            } catch (IOException e) {
                throw ErreurMetier.validation("Le fichier « " + nom + " » n'a pas pu être lu : renvoyez-le.");
            }
        }
        return resultat;
    }

    /** Nom sans chemin (certains navigateurs envoient C:\...\cv.pdf) ni caractère de contrôle. */
    private static String nomFichier(String original) {
        String nom = original == null ? "" : original;
        nom = nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1);
        nom = nom.replaceAll("\\p{Cntrl}", "").trim();
        if (nom.isEmpty()) {
            throw ErreurMetier.validation("Un fichier n'a pas de nom.");
        }
        if (nom.length() > NOM_FICHIER_MAX) {
            throw ErreurMetier.validation("Le nom du fichier « " + nom.substring(0, 40) + "… » dépasse "
                    + NOM_FICHIER_MAX + " caractères.");
        }
        return nom;
    }

    /** Type envoyé par le navigateur s'il est valide, sinon application/octet-stream. */
    private static String type(String type) {
        String t = LectureDossier.typeValide(type);
        return t.length() > TYPE_MAX ? "application/octet-stream" : t;
    }

    private static String enMo(long octets) {
        return String.format(Locale.FRANCE, "%.1f Mo", octets / 1024.0 / 1024.0);
    }

    // ---------- Outils ----------

    private static Integer entier(String texte, String siInvalide) {
        try {
            return Integer.valueOf(texte.trim());
        } catch (NumberFormatException e) {
            throw ErreurMetier.validation(siInvalide);
        }
    }

    /** Pour comparer deux noms comme le front (utils/texte.js) : sans accents ni majuscules. */
    private static String sansAccent(String s) {
        return Normalizer.normalize(s == null ? "" : s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
