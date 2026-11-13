package gestion.hr.entretien.Dto;

/**
 * Un poste qui fait (ou peut faire) un entretien : le poste d'un compte, son département, le détenteur du poste
 * (« Prénom Nom », null si vacant) et le login du compte (affichage et recherche seulement : les droits viennent
 * de monPoste / peutEvaluer dans EntretienResponse).
 */
public record InterviewerResponse(
        Integer idPoste,
        String poste,
        Integer idDepartement,
        String departement,
        String detenteur,
        String login) {
}
