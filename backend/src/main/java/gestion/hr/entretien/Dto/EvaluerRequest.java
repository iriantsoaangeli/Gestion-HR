package gestion.hr.entretien.Dto;

import java.math.BigDecimal;

/**
 * Corps de POST /api/entretiens/{id}/evaluation. L'évaluateur n'est pas dans le JSON : c'est le poste
 * du compte connecté. note : BigDecimal pour refuser 50.5 (un Integer l'arrondirait sans rien dire).
 */
public record EvaluerRequest(
        BigDecimal note,
        String remarques) {
}
