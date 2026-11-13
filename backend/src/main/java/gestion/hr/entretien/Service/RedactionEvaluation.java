package gestion.hr.entretien.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.commun.Validation.Texte;
import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.EvaluerRequest;
import gestion.hr.entretien.Model.Entretien;
import gestion.hr.entretien.Model.Evaluation;
import gestion.hr.entretien.Model.StatutEntretien;
import gestion.hr.entretien.Repository.EvaluationRepository;
import gestion.hr.entretien.Repository.InterviewerRepository;

/**
 * Évaluer un entretien (appelé seulement par EntretienService, qui porte la transaction).
 * - L'évaluateur = le poste du compte connecté (utilisateur.idPoste), jamais le JSON ; il doit être interviewer.
 * - Une seule évaluation par interviewer (409 DEJA_FAIT) ; note entière de 0 à 100 ; remarques obligatoires.
 * - Pas avant la date de l'entretien (409 ENTRETIEN_A_VENIR) ; possible même si l'offre est close depuis.
 * - Quand tous les interviewers ont évalué, l'entretien passe FINI.
 */
@Component
class RedactionEvaluation {

    static final int REMARQUES_MAX = 5000;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final LectureEntretien lecture;
    private final InterviewerRepository interviewers;
    private final EvaluationRepository evaluations;
    private final AccesEntretien acces;
    private final ConversionEntretien conversion;

    RedactionEvaluation(LectureEntretien lecture, InterviewerRepository interviewers,
            EvaluationRepository evaluations, AccesEntretien acces, ConversionEntretien conversion) {
        this.lecture = lecture;
        this.interviewers = interviewers;
        this.evaluations = evaluations;
        this.acces = acces;
        this.conversion = conversion;
    }

    EntretienResponse evaluer(Integer id, EvaluerRequest req) {
        Utilisateur moi = acces.lecteur();
        Entretien e = lecture.trouver(id);
        if (!interviewers.existsByIdEntretienAndIdPoste(id, moi.getIdPoste())) {
            throw ErreurMetier.accesRefuse(AccesEntretien.PAS_INTERVIEWER);
        }
        if (evaluations.findByIdEntretienAndIdPoste(id, moi.getIdPoste()).isPresent()) {
            throw ErreurMetier.dejaFait(AccesEntretien.DEJA_EVALUE);
        }
        if (e.getDate().isAfter(LocalDateTime.now())) {
            throw new ErreurMetier(HttpStatus.CONFLICT, "ENTRETIEN_A_VENIR", AccesEntretien.A_VENIR);
        }

        BigDecimal n = req.note();
        if (n == null || n.signum() < 0 || n.compareTo(CENT) > 0 || n.stripTrailingZeros().scale() > 0) {
            throw ErreurMetier.validation("La note est un nombre entier de 0 à 100.");
        }
        String remarques = Texte.obligatoire(req.remarques(), "Les remarques", REMARQUES_MAX);

        evaluations.save(new Evaluation(id, moi.getIdPoste(), n.intValueExact(), remarques));
        if (evaluations.countByIdEntretien(id) >= interviewers.countByIdEntretien(id)) {
            e.setStatut(StatutEntretien.FINI);
        }
        return conversion.enReponse(e, moi);
    }
}
