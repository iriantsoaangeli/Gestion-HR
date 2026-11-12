package gestion.hr.demande.Dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import gestion.hr.demande.Model.Statut;
import gestion.hr.referentiel.Dto.PersonnelResponse;

/**
 * Demande renvoyée au front, avec tout ce qu'il faut pour l'afficher (libellés compris).
 * etapeValideur : rôle qui doit décider maintenant (HR, DIRECTION, FINANCE), null si la demande est terminée.
 * aMoiDeDecider : c'est à l'utilisateur connecté de valider ou refuser maintenant.
 * suivi : une ligne par étape du workflow, déjà calculée (barre de progression et bloc Suivi du front),
 *         avec le personnel de l'étape (détenteur du poste et rôle).
 */
public record DemandeResponse(
        Integer id,
        Integer idDepartement,
        String departement,
        Statut statut,
        String statutLibelle,
        String etapeValideur,
        Integer idDemandeur,
        String demandeur,
        LocalDateTime date,
        String motif,
        LocalDate delai,
        List<Creation> creations,
        List<Remplacement> remplacements,
        List<Decision> historique,
        boolean aMoiDeDecider,
        List<Etape> suivi) {

    public record Creation(Integer idFonction, String fonction, Integer effectif) {
    }

    public record Remplacement(Integer idPoste, String poste) {
    }

    /**
     * etat : faite, courante, refusee ou a-venir ; texte : « Validée », « En attente depuis »…
     * utilisateur : login de celui qui a agi (null si l'étape n'est pas faite).
     * personnel : étape faite = celui qui a créé / décidé ; sinon les comptes du rôle qui décidera.
     */
    public record Etape(String libelle, String etat, String texte, LocalDateTime date, String utilisateur,
            List<PersonnelResponse> personnel) {
    }

    /** Une validation (GET /api/demandes/{id}/validateurs) : qui a validé, et quand. */
    public record Validation(LocalDateTime date, Statut statut, PersonnelResponse personnel) {
    }

    /** statut = statut obtenu après la décision (étape suivante, APPROUVEE ou REFUSEE). */
    public record Decision(String utilisateur, String role, Statut statut, LocalDateTime date) {
    }
}
