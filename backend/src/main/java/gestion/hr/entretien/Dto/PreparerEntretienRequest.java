package gestion.hr.entretien.Dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corps de POST /api/entretiens (HR) : le dossier du candidat, la date-heure (locale, dans le futur)
 * et les postes des interviewers (au moins un, parmi GET /api/entretiens/interviewers-possibles).
 */
public record PreparerEntretienRequest(
        Integer idDossier,
        LocalDateTime date,
        List<Integer> interviewers) {
}
