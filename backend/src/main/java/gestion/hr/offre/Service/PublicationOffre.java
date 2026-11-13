package gestion.hr.offre.Service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.commun.Validation.Texte;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Model.Statut;
import gestion.hr.demande.Service.DemandeService;
import gestion.hr.offre.Dto.OffreResponse;
import gestion.hr.offre.Dto.PublierOffreRequest;
import gestion.hr.offre.Model.CreationPoste;
import gestion.hr.offre.Model.Emploi;
import gestion.hr.offre.Model.FonctionPoste;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Model.OffrePoste;
import gestion.hr.offre.Repository.CreationPosteRepository;
import gestion.hr.offre.Repository.EmploiRepository;
import gestion.hr.offre.Repository.FonctionPosteRepository;
import gestion.hr.offre.Repository.OffrePosteRepository;
import gestion.hr.offre.Repository.OffreRepository;
import gestion.hr.referentiel.Model.Departement;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Publier une offre (appelé seulement par OffreService, qui porte la transaction).
 * - Seul HR publie, depuis une demande APPROUVEE et UNE de ses lignes : une création ou un remplacement.
 * - Une ligne déjà couverte par une offre (ouverte ou close) ne se republie pas (409 DEJA_FAIT).
 * - Création : effectif postes vides dans le département de la demande (label = la fonction), chacun avec
 *   fonctionPoste, emploi vacant et creationPoste (comme base/demo.sql), code lisible calculé par CodePoste.
 * - Remplacement : l'offre couvre le poste existant.
 * - Responsable = poste du manager qui a fait la demande ; émetteur = l'utilisateur connecté.
 */
@Component
class PublicationOffre {

    static final int INTITULE_MAX = 150;
    static final int TEXTE_MAX = 4000;

    private final OffreRepository offres;
    private final OffrePosteRepository offrePostes;
    private final PosteRepository postes;
    private final FonctionPosteRepository fonctionsPostes;
    private final EmploiRepository emplois;
    private final CreationPosteRepository creationsPostes;
    private final DepartementRepository departements;
    private final UtilisateurRepository utilisateurs;
    private final AccesOffre acces;
    private final DemandeService demandes;
    private final CouvertureDemande couverture;
    private final CodePoste codes;
    private final ConversionOffre conversion;

    PublicationOffre(OffreRepository offres, OffrePosteRepository offrePostes, PosteRepository postes,
            FonctionPosteRepository fonctionsPostes, EmploiRepository emplois,
            CreationPosteRepository creationsPostes, DepartementRepository departements,
            UtilisateurRepository utilisateurs, AccesOffre acces, DemandeService demandes,
            CouvertureDemande couverture, CodePoste codes, ConversionOffre conversion) {
        this.offres = offres;
        this.offrePostes = offrePostes;
        this.postes = postes;
        this.fonctionsPostes = fonctionsPostes;
        this.emplois = emplois;
        this.creationsPostes = creationsPostes;
        this.departements = departements;
        this.utilisateurs = utilisateurs;
        this.acces = acces;
        this.demandes = demandes;
        this.couverture = couverture;
        this.codes = codes;
        this.conversion = conversion;
    }

    OffreResponse publier(PublierOffreRequest req) {
        Utilisateur moi = acces.exigerHr();
        String intitule = Texte.obligatoire(req.intitule(), "L'intitulé de l'offre", INTITULE_MAX);
        String description = Texte.obligatoire(req.description(), "La description de l'offre", TEXTE_MAX);
        String profil = Texte.facultatif(req.profil(), "Le profil recherché", TEXTE_MAX);
        if (req.delai() != null && !req.delai().isAfter(LocalDate.now())) {
            throw ErreurMetier.validation("La date limite de candidature doit être au moins demain.");
        }

        DemandeResponse d = demandeApprouvee(req.idDemande());
        if ((req.idFonction() == null) == (req.idPoste() == null)) {
            throw ErreurMetier.validation(
                    "Choisissez un seul poste de la demande : une création ou un remplacement.");
        }
        CouvertureDemande.Couverture c = couverture.de(d);
        Integer idPosteResponsable = utilisateurs.findById(d.idDemandeur()).map(Utilisateur::getIdPoste)
                .orElseThrow(() -> ErreurMetier.validation("Le manager de la demande n° " + d.id() + " n'existe plus."));

        // La ligne choisie : une création (idFonction) ou un remplacement (idPoste), pas encore publiée
        DemandeResponse.Creation creation = req.idFonction() == null ? null : creation(d, c, req.idFonction());
        DemandeResponse.Remplacement remplacement = creation != null ? null : remplacement(d, c, req.idPoste());

        Offre o = offres.save(new Offre(intitule, req.delai(), idPosteResponsable, description, profil,
                d.id(), moi.getId()));
        if (creation != null) {
            creerPostes(o, d, creation);
        } else {
            offrePostes.save(new OffrePoste(o.getId(), remplacement.idPoste()));
        }
        return conversion.enReponse(o);
    }

    private static DemandeResponse.Creation creation(DemandeResponse d, CouvertureDemande.Couverture c,
            Integer idFonction) {
        DemandeResponse.Creation ligne = d.creations().stream()
                .filter(x -> idFonction.equals(x.idFonction())).findFirst()
                .orElseThrow(() -> ErreurMetier.validation(
                        "Cette création ne fait pas partie de la demande n° " + d.id() + "."));
        dejaPubliee(c.creations().get(ligne.idFonction()), ligne.fonction(), d);
        return ligne;
    }

    private static DemandeResponse.Remplacement remplacement(DemandeResponse d, CouvertureDemande.Couverture c,
            Integer idPoste) {
        DemandeResponse.Remplacement ligne = d.remplacements().stream()
                .filter(x -> idPoste.equals(x.idPoste())).findFirst()
                .orElseThrow(() -> ErreurMetier.validation(
                        "Ce poste à remplacer ne fait pas partie de la demande n° " + d.id() + "."));
        dejaPubliee(c.remplacements().get(ligne.idPoste()), ligne.poste(), d);
        return ligne;
    }

    /** Un poste vide par effectif, dans le département de la demande, relié à l'offre. */
    private void creerPostes(Offre o, DemandeResponse d, DemandeResponse.Creation ligne) {
        Departement dep = departements.findById(d.idDepartement())
                .orElseThrow(() -> ErreurMetier.validation("Le département de la demande n° " + d.id() + " n'existe plus."));
        for (String code : codes.suivants(dep.getCode(), ligne.effectif())) {
            Poste p = postes.save(new Poste(code, ligne.fonction(), dep.getId()));
            fonctionsPostes.save(new FonctionPoste(p.getId(), ligne.idFonction()));
            emplois.save(Emploi.vacant(p.getId()));
            creationsPostes.save(new CreationPoste(p.getId()));
            offrePostes.save(new OffrePoste(o.getId(), p.getId()));
        }
    }

    /** La demande, si elle existe et est approuvée. */
    private DemandeResponse demandeApprouvee(Integer idDemande) {
        if (idDemande == null) {
            throw ErreurMetier.validation("Choisissez une demande approuvée.");
        }
        DemandeResponse d = demandes.pourOffre(idDemande)
                .orElseThrow(() -> ErreurMetier.validation("La demande n° " + idDemande + " n'existe pas."));
        if (d.statut() != Statut.APPROUVEE) {
            throw ErreurMetier.validation("La demande n° " + idDemande + " n'est pas approuvée ("
                    + d.statutLibelle() + ") : elle ne peut pas encore donner d'offre.");
        }
        return d;
    }

    private static void dejaPubliee(Integer idOffre, String poste, DemandeResponse d) {
        if (idOffre != null) {
            throw ErreurMetier.dejaFait("Le poste « " + poste + " » de la demande n° " + d.id()
                    + " a déjà une offre (n° " + idOffre + ").");
        }
    }
}
