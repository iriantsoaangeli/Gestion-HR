package gestion.hr.demande.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Model.Demande;
import gestion.hr.demande.Model.HistoDemande;
import gestion.hr.demande.Model.Statut;
import gestion.hr.demande.Repository.DemandeRepository;
import gestion.hr.demande.Repository.HistoDemandeRepository;

/**
 * Valider ou refuser une demande (appelé seulement par DemandeService, qui porte la transaction).
 * - À chaque étape, seul le rôle de l'étape (Statut.getValideur) décide.
 * - Chaque décision est écrite dans histoDemande, dans la même transaction que le changement de statut.
 */
@Component
class DecisionDemande {

    private final DemandeRepository demandes;
    private final HistoDemandeRepository historique;
    private final UtilisateurCourant courant;
    private final ConversionDemande conversion;

    DecisionDemande(DemandeRepository demandes, HistoDemandeRepository historique, UtilisateurCourant courant,
            ConversionDemande conversion) {
        this.demandes = demandes;
        this.historique = historique;
        this.courant = courant;
        this.conversion = conversion;
    }

    DemandeResponse decider(Integer id, boolean valide) {
        Utilisateur moi = courant.get();
        Demande d = demandes.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("La demande n° " + id + " n'existe pas."));

        Statut actuel = d.getStatut();
        if (!actuel.estEnAttente()) {
            throw new ErreurMetier(HttpStatus.CONFLICT, "TRANSITION_INTERDITE",
                    "La demande n° " + id + " est déjà traitée (" + actuel.getLibelle() + ").");
        }
        if (actuel.getValideur() != moi.getRole()) {
            throw ErreurMetier.accesRefuse("Cette demande attend la décision de l'étape "
                    + actuel.getValideur().getLibelle() + ".");
        }

        Statut nouveau = valide ? actuel.apresValidation() : Statut.REFUSEE;
        d.setStatut(nouveau);
        historique.save(new HistoDemande(d.getId(), moi.getId(), nouveau));
        return conversion.enReponse(d, moi);
    }
}
