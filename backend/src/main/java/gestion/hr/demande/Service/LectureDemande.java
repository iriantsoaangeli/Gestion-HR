package gestion.hr.demande.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Model.Demande;
import gestion.hr.demande.Model.Statut;
import gestion.hr.demande.Repository.DemandeRepository;
import gestion.hr.demande.Repository.HistoDemandeRepository;
import gestion.hr.referentiel.Service.ReferentielService;

/**
 * Lire les demandes (appelé seulement par DemandeService, qui porte la transaction).
 * Une demande n'est visible que par son demandeur, ceux qui ont décidé dessus, le rôle dont c'est le tour,
 * et ADMIN / DIRECTION (toutes les demandes).
 */
@Component
class LectureDemande {

    private final DemandeRepository demandes;
    private final HistoDemandeRepository historique;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurCourant courant;
    private final ReferentielService referentiel;
    private final ConversionDemande conversion;

    LectureDemande(DemandeRepository demandes, HistoDemandeRepository historique, UtilisateurRepository utilisateurs,
            UtilisateurCourant courant, ReferentielService referentiel, ConversionDemande conversion) {
        this.demandes = demandes;
        this.historique = historique;
        this.utilisateurs = utilisateurs;
        this.courant = courant;
        this.referentiel = referentiel;
        this.conversion = conversion;
    }

    /** Une demande, si je peux la voir (sinon 403). */
    DemandeResponse detail(Integer id) {
        Utilisateur moi = courant.get();
        return conversion.enReponse(visible(id, moi), moi);
    }

    /** Personnel ayant validé la demande (les refus n'y sont pas), dans l'ordre du workflow. Même accès que detail. */
    List<DemandeResponse.Validation> validateurs(Integer id) {
        Demande d = visible(id, courant.get());
        return historique.findByIdDemandeOrderByDateAscIdAsc(d.getId()).stream()
                .filter(x -> x.getStatut() != Statut.REFUSEE)
                .map(x -> new DemandeResponse.Validation(x.getDate(), x.getStatut(),
                        referentiel.personnel(utilisateurs.findById(x.getIdUtilisateur()).orElse(null))))
                .toList();
    }

    /** Toutes les demandes : ADMIN et DIRECTION seulement. */
    List<DemandeResponse> toutes() {
        Utilisateur moi = courant.get();
        if (!moi.getRole().voitToutesLesDemandes()) {
            throw ErreurMetier.accesRefuse("Seules l'administration et la direction voient toutes les demandes.");
        }
        return conversion.enReponses(demandes.findAllByOrderByDateDescIdDesc(), moi);
    }

    /** Demandes que j'ai créées. */
    List<DemandeResponse> mesDemandes() {
        Utilisateur moi = courant.get();
        return conversion.enReponses(demandes.findByIdDemandeurOrderByDateDescIdDesc(moi.getId()), moi);
    }

    /** Demandes qui attendent ma décision (les plus anciennes d'abord). Vide si mon rôle ne valide pas. */
    List<DemandeResponse> aTraiter() {
        Utilisateur moi = courant.get();
        List<Statut> statuts = Statut.enAttenteDe(moi.getRole());
        return statuts.isEmpty() ? List.of()
                : conversion.enReponses(demandes.findByStatutInOrderByDateAscIdAsc(statuts), moi);
    }

    /** Demandes sur lesquelles j'ai pris une décision (validée ou refusée). */
    List<DemandeResponse> traitees() {
        Utilisateur moi = courant.get();
        return conversion.enReponses(demandes.findTraiteesPar(moi.getId()), moi);
    }

    /** Étapes du workflow pour une demande pas encore créée (progression du formulaire). */
    List<DemandeResponse.Etape> etapes() {
        List<DemandeResponse.Etape> etapes = new ArrayList<>();
        etapes.add(new DemandeResponse.Etape(Role.MANAGER.getLibelle(), "courante", "Création de la demande", null, null,
                ConversionDemande.seul(referentiel.personnel(courant.get()))));
        for (Statut s : Statut.etapesDeValidation()) {
            etapes.add(new DemandeResponse.Etape(s.getValideur().getLibelle(), "a-venir", "À venir", null, null,
                    referentiel.personnelDuRole(s.getValideur())));
        }
        return etapes;
    }

    /** La demande, si je peux la voir ; sinon 403 (404 si elle n'existe pas). */
    private Demande visible(Integer id, Utilisateur moi) {
        Demande d = demandes.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("La demande n° " + id + " n'existe pas."));
        boolean visible = d.getIdDemandeur().equals(moi.getId())
                || moi.getRole().voitToutesLesDemandes()
                || d.getStatut().getValideur() == moi.getRole()
                || historique.existsByIdDemandeAndIdUtilisateur(d.getId(), moi.getId());
        if (!visible) {
            throw ErreurMetier.accesRefuse("Vous n'avez pas accès à la demande n° " + id + ".");
        }
        return d;
    }
}
