package gestion.hr.entretien.Dto;

import java.time.LocalDateTime;
import java.util.List;

import gestion.hr.entretien.Model.StatutEntretien;

/**
 * Entretien renvoyé au front, avec tout ce qu'il faut pour l'afficher.
 * poste = intitulé de l'offre ; departement = département des postes de l'offre ; candidat = « Prénom Nom ».
 * fini = statut FINI (tous les interviewers ont évalué).
 * note = moyenne arrondie des évaluations (null sans évaluation).
 * Notes séparées (notesCachees = true) : je suis interviewer et je n'ai pas encore évalué ; alors note, et
 * note / remarques de chaque évaluation, valent null.
 * Pour l'utilisateur connecté : monPoste = mon poste s'il est interviewer (sinon null) ; peutEvaluer ;
 * raisonEvaluer = pourquoi je ne peux pas (null si je peux) ; maNote / idEvaluation = mon évaluation (ou null).
 */
public record EntretienResponse(
        Integer id,
        LocalDateTime date,
        Integer idDossier,
        Integer idOffre,
        String poste,
        Integer idDepartement,
        String departement,
        Integer idPersonne,
        String candidat,
        StatutEntretien statut,
        String statutLibelle,
        boolean fini,
        List<InterviewerResponse> interviewers,
        List<EvaluationResponse> evaluations,
        Integer note,
        boolean notesCachees,
        Integer monPoste,
        boolean peutEvaluer,
        String raisonEvaluer,
        Integer maNote,
        Integer idEvaluation) {
}
