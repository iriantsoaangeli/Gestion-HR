package gestion.hr.entretien.Service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.dossier.Model.Dossier;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.EvaluationResponse;
import gestion.hr.entretien.Dto.FicheEvaluationResponse;
import gestion.hr.entretien.Dto.InterviewerResponse;
import gestion.hr.entretien.Model.Entretien;
import gestion.hr.entretien.Model.Evaluation;
import gestion.hr.entretien.Model.StatutEntretien;
import gestion.hr.entretien.Repository.EntretienRepository;
import gestion.hr.entretien.Repository.EvaluationRepository;
import gestion.hr.offre.Repository.OffreRepository;

/**
 * Lire les entretiens et les évaluations (appelé seulement par EntretienService, qui porte la transaction).
 * Lecture réservée aux rôles d'AccesEntretien.ROLES ; les notes séparées sont appliquées par ConversionEntretien.
 */
@Component
class LectureEntretien {

    private final EntretienRepository entretiens;
    private final EvaluationRepository evaluations;
    private final DossierRepository dossiers;
    private final OffreRepository offres;
    private final AccesEntretien acces;
    private final ConversionEntretien conversion;

    LectureEntretien(EntretienRepository entretiens, EvaluationRepository evaluations, DossierRepository dossiers,
            OffreRepository offres, AccesEntretien acces, ConversionEntretien conversion) {
        this.entretiens = entretiens;
        this.evaluations = evaluations;
        this.dossiers = dossiers;
        this.offres = offres;
        this.acces = acces;
        this.conversion = conversion;
    }

    /** Entretiens en attente, les plus proches d'abord. */
    List<EntretienResponse> enAttente() {
        Utilisateur moi = acces.lecteur();
        return conversion.enReponses(entretiens.findByStatutOrderByDateAscIdAsc(StatutEntretien.EN_ATTENTE), moi);
    }

    /** Entretiens finis (tous les interviewers ont évalué), les plus récents d'abord. */
    List<EntretienResponse> finis() {
        Utilisateur moi = acces.lecteur();
        return conversion.enReponses(entretiens.findByStatutOrderByDateDescIdDesc(StatutEntretien.FINI), moi);
    }

    /** Tous les entretiens, ou ceux d'une offre (idOffre non null ; 404 si l'offre n'existe pas). */
    List<EntretienResponse> liste(Integer idOffre) {
        Utilisateur moi = acces.lecteur();
        if (idOffre == null) {
            return conversion.enReponses(entretiens.findAllByOrderByDateAscIdAsc(), moi);
        }
        if (!offres.existsById(idOffre)) {
            throw ErreurMetier.introuvable("L'offre n° " + idOffre + " n'existe pas.");
        }
        List<Integer> idDossiers = dossiers.findByIdOffre(idOffre).stream().map(Dossier::getId).toList();
        return idDossiers.isEmpty() ? List.of()
                : conversion.enReponses(entretiens.findByIdDossierInOrderByDateAscIdAsc(idDossiers), moi);
    }

    EntretienResponse detail(Integer id) {
        Utilisateur moi = acces.lecteur();
        return conversion.enReponse(trouver(id), moi);
    }

    /** Entretiens dont je suis interviewer et que je n'ai pas encore évalués. */
    List<EntretienResponse> aFaire() {
        Utilisateur moi = acces.lecteur();
        return conversion.enReponses(entretiens.aEvaluerPar(moi.getIdPoste()), moi);
    }

    /** Entretiens que j'ai déjà évalués (maNote et idEvaluation remplis). */
    List<EntretienResponse> faites() {
        Utilisateur moi = acces.lecteur();
        return conversion.enReponses(entretiens.evaluesPar(moi.getIdPoste()), moi);
    }

    /** Fiche d'une évaluation ; refusée tant que les notes de cet entretien me sont cachées. */
    FicheEvaluationResponse fiche(Integer id) {
        Utilisateur moi = acces.lecteur();
        Evaluation v = evaluations.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("L'évaluation n° " + id + " n'existe pas."));
        EntretienResponse e = conversion.enReponse(trouver(v.getIdEntretien()), moi);
        if (e.notesCachees()) {
            throw ErreurMetier.accesRefuse("Rendez d'abord votre évaluation pour voir celles des autres interviewers.");
        }
        EvaluationResponse r = conversion.evaluation(v, false);
        return new FicheEvaluationResponse(r.id(), r.idPoste(), r.poste(), r.departement(), r.detenteur(), r.login(),
                r.date(), r.note(), r.remarques(), v.getIdPoste().equals(moi.getIdPoste()), e);
    }

    /** Postes qui peuvent faire un entretien (formulaire de préparation, HR), triés par libellé. */
    List<InterviewerResponse> interviewersPossibles() {
        acces.exigerHr();
        return acces.postesPossibles().stream().map(conversion::interviewer)
                .sorted(Comparator.comparing(InterviewerResponse::poste, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Un entretien (404 s'il n'existe pas). */
    Entretien trouver(Integer id) {
        return entretiens.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("L'entretien n° " + id + " n'existe pas."));
    }
}
