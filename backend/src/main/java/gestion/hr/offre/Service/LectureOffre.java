package gestion.hr.offre.Service;

import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Service.DemandeService;
import gestion.hr.offre.Dto.DemandeApprouveeResponse;
import gestion.hr.offre.Dto.OffreResponse;
import gestion.hr.offre.Repository.OffreRepository;
import gestion.hr.referentiel.Dto.PersonnelResponse;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.PosteRepository;
import gestion.hr.referentiel.Service.ReferentielService;

/**
 * Lire les offres (appelé seulement par OffreService, qui porte la transaction). Tout est réservé à HR.
 * Demandes approuvées : toutes, chaque ligne avec l'offre qui la couvre déjà (idOffre, null si libre).
 */
@Component
class LectureOffre {

    private final OffreRepository offres;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;
    private final AccesOffre acces;
    private final DemandeService demandes;
    private final ReferentielService referentiel;
    private final CouvertureDemande couverture;
    private final ConversionOffre conversion;

    LectureOffre(OffreRepository offres, PosteRepository postes, UtilisateurRepository utilisateurs,
            AccesOffre acces, DemandeService demandes, ReferentielService referentiel,
            CouvertureDemande couverture, ConversionOffre conversion) {
        this.offres = offres;
        this.postes = postes;
        this.utilisateurs = utilisateurs;
        this.acces = acces;
        this.demandes = demandes;
        this.referentiel = referentiel;
        this.couverture = couverture;
        this.conversion = conversion;
    }

    /** Toutes les offres, les plus récentes d'abord. */
    List<OffreResponse> offres() {
        acces.exigerHr();
        return offres.findAllByOrderByIdDesc().stream().map(conversion::enReponse).toList();
    }

    /** Une offre (404 si elle n'existe pas). */
    OffreResponse offre(Integer id) {
        acces.exigerHr();
        return conversion.enReponse(offres.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("L'offre n° " + id + " n'existe pas.")));
    }

    /** Demandes approuvées pour le formulaire de création, les plus récentes d'abord. */
    List<DemandeApprouveeResponse> demandesApprouvees() {
        acces.exigerHr();
        return demandes.approuvees().stream().map(this::enDemande).toList();
    }

    private DemandeApprouveeResponse enDemande(DemandeResponse d) {
        CouvertureDemande.Couverture c = couverture.de(d);
        Utilisateur u = utilisateurs.findById(d.idDemandeur()).orElse(null);
        PersonnelResponse p = referentiel.personnel(u);
        DemandeApprouveeResponse.Demandeur demandeur = new DemandeApprouveeResponse.Demandeur(d.demandeur(),
                p == null ? null : p.detenteur());
        return new DemandeApprouveeResponse(d.id(), d.date(), d.delai(), d.idDepartement(), d.departement(),
                demandeur, d.motif(),
                d.creations().stream().map(x -> new DemandeApprouveeResponse.Creation(x.idFonction(), x.fonction(),
                        x.effectif(), c.creations().get(x.idFonction()))).toList(),
                d.remplacements().stream().map(x -> new DemandeApprouveeResponse.Remplacement(x.idPoste(), x.poste(),
                        postes.findById(x.idPoste()).map(Poste::getCode).orElse(null),
                        c.remplacements().get(x.idPoste()))).toList());
    }
}
