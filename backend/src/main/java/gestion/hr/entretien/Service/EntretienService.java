package gestion.hr.entretien.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.EvaluerRequest;
import gestion.hr.entretien.Dto.FicheEvaluationResponse;
import gestion.hr.entretien.Dto.InterviewerResponse;
import gestion.hr.entretien.Dto.PreparerEntretienRequest;

/**
 * Point d'entrée unique des entretiens et des évaluations (doc/routes/api/entretiens.md).
 * Chaque section est dans sa classe du même package, invisible hors de celui-ci :
 * - AccesEntretien       : qui lit (tous sauf FINANCE et ADMIN), qui prépare (HR), postes interviewers possibles
 * - PreparationEntretien : préparer un entretien (HR)
 * - RedactionEvaluation  : évaluer (l'interviewer connecté), passage à FINI
 * - LectureEntretien     : listes, fiche d'entretien, mes évaluations, fiche d'évaluation
 * - ConversionEntretien  : Entretien -> EntretienResponse (notes séparées, droit d'évaluer)
 * Les transactions sont ouvertes ici.
 */
@Service
public class EntretienService {

    private final PreparationEntretien preparation;
    private final RedactionEvaluation redaction;
    private final LectureEntretien lecture;

    EntretienService(PreparationEntretien preparation, RedactionEvaluation redaction, LectureEntretien lecture) {
        this.preparation = preparation;
        this.redaction = redaction;
        this.lecture = lecture;
    }

    // ---------- Préparer / évaluer ----------

    @Transactional
    public EntretienResponse preparer(PreparerEntretienRequest req) {
        return preparation.preparer(req);
    }

    @Transactional
    public EntretienResponse evaluer(Integer id, EvaluerRequest req) {
        return redaction.evaluer(id, req);
    }

    // ---------- Lire les entretiens ----------

    @Transactional(readOnly = true)
    public List<EntretienResponse> enAttente() {
        return lecture.enAttente();
    }

    @Transactional(readOnly = true)
    public List<EntretienResponse> finis() {
        return lecture.finis();
    }

    @Transactional(readOnly = true)
    public List<EntretienResponse> liste(Integer idOffre) {
        return lecture.liste(idOffre);
    }

    @Transactional(readOnly = true)
    public EntretienResponse detail(Integer id) {
        return lecture.detail(id);
    }

    @Transactional(readOnly = true)
    public List<InterviewerResponse> interviewersPossibles() {
        return lecture.interviewersPossibles();
    }

    // ---------- Mes évaluations ----------

    @Transactional(readOnly = true)
    public List<EntretienResponse> aFaire() {
        return lecture.aFaire();
    }

    @Transactional(readOnly = true)
    public List<EntretienResponse> faites() {
        return lecture.faites();
    }

    @Transactional(readOnly = true)
    public FicheEvaluationResponse fiche(Integer id) {
        return lecture.fiche(id);
    }
}
