package gestion.hr.Dto;

import java.time.LocalDate;
import java.util.List;

import gestion.hr.Model.Statut;

/**
 * Demande renvoyée au front, avec tout ce qu'il faut pour l'afficher (libellés compris).
 * etapeValideur : rôle qui doit décider maintenant (HR, DIRECTION, FINANCE), null si la demande est terminée.
 * aMoiDeDecider : c'est à l'utilisateur connecté de valider ou refuser maintenant.
 * suivi : une ligne par étape du workflow, déjà calculée (barre de progression et bloc Suivi du front).
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
        LocalDate date,
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

    /** etat : faite, courante, refusee ou a-venir ; texte : « Validée », « En attente depuis »… */
    public record Etape(String libelle, String etat, String texte, LocalDate date, String utilisateur) {
    }

    /** statut = statut obtenu après la décision (étape suivante, APPROUVEE ou REFUSEE). */
    public record Decision(String utilisateur, String role, Statut statut, LocalDate date) {
    }
}
