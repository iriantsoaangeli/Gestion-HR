package gestion.hr.entretien.Dto;

import java.time.LocalDateTime;

/**
 * Une évaluation d'entretien avec son évaluateur (poste, département, détenteur, login).
 * Notes séparées : note et remarques valent null quand le lecteur est un interviewer qui n'a pas encore évalué.
 */
public record EvaluationResponse(
        Integer id,
        Integer idPoste,
        String poste,
        String departement,
        String detenteur,
        String login,
        LocalDateTime date,
        Integer note,
        String remarques) {
}
