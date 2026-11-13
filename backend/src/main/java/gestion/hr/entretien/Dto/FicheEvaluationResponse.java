package gestion.hr.entretien.Dto;

import java.time.LocalDateTime;

/**
 * Fiche d'une évaluation (GET /api/evaluations/{id}) : l'évaluation, son entretien, et deMoi = je l'ai écrite.
 */
public record FicheEvaluationResponse(
        Integer id,
        Integer idPoste,
        String poste,
        String departement,
        String detenteur,
        String login,
        LocalDateTime date,
        Integer note,
        String remarques,
        boolean deMoi,
        EntretienResponse entretien) {
}
