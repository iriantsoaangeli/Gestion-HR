package gestion.hr.referentiel.Dto;

/**
 * Une personne du personnel qui utilise l'application : son compte, son rôle, son poste et le détenteur du poste.
 * detenteur = « Prénom Nom » de la personne qui occupe le poste (null si le poste est vacant).
 */
public record PersonnelResponse(
        Integer idUtilisateur,
        String login,
        String role,
        String roleLibelle,
        Integer idPoste,
        String poste,
        String detenteur) {
}
