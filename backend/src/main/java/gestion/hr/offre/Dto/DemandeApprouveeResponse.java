package gestion.hr.offre.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Demande approuvée proposée dans le formulaire de création d'offre (GET /api/offres/demandes-approuvees).
 * Chaque ligne (création ou remplacement) porte idOffre : l'offre qui la couvre déjà, null si elle est libre.
 */
public record DemandeApprouveeResponse(
        Integer id,
        LocalDateTime date,
        LocalDate delai,
        Integer idDepartement,
        String departement,
        Demandeur demandeur,
        String motif,
        List<Creation> creations,
        List<Remplacement> remplacements) {

    /** detenteur = « Prénom Nom » de la personne qui occupe le poste du manager (null si vacant). */
    public record Demandeur(String login, String detenteur) {
    }

    public record Creation(Integer idFonction, String fonction, Integer effectif, Integer idOffre) {
    }

    public record Remplacement(Integer idPoste, String poste, String code, Integer idOffre) {
    }
}
