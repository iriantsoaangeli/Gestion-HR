package gestion.hr.entretien.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.dossier.Model.Dossier;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.EvaluationResponse;
import gestion.hr.entretien.Dto.InterviewerResponse;
import gestion.hr.entretien.Model.Entretien;
import gestion.hr.entretien.Model.Evaluation;
import gestion.hr.entretien.Model.Interviewer;
import gestion.hr.entretien.Model.StatutEntretien;
import gestion.hr.entretien.Repository.EvaluationRepository;
import gestion.hr.entretien.Repository.InterviewerRepository;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Repository.OffreRepository;
import gestion.hr.offre.Service.DepartementOffre;
import gestion.hr.referentiel.Model.Departement;
import gestion.hr.referentiel.Model.Personne;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.PersonneRepository;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Entretien -> EntretienResponse, vu par l'utilisateur connecté (utilisé par les autres classes du package).
 * - Offre et candidat sont lus par le dossier ; département de l'offre = DepartementOffre (même règle que la
 *   liste des offres).
 * - Notes séparées : un interviewer qui n'a pas encore évalué ne reçoit ni les notes ni les remarques des autres,
 *   ni la moyenne (notesCachees = true). Il voit seulement qui a évalué.
 * - Droit d'évaluer calculé ici (monPoste, peutEvaluer, raisonEvaluer) : le front n'a rien à comparer.
 *   Raisons, dans l'ordre : pas interviewer, déjà évalué, entretien pas encore eu lieu (AccesEntretien).
 */
@Component
class ConversionEntretien {

    private final InterviewerRepository interviewers;
    private final EvaluationRepository evaluations;
    private final DossierRepository dossiers;
    private final OffreRepository offres;
    private final DepartementOffre departementOffre;
    private final PosteRepository postes;
    private final DepartementRepository departements;
    private final PersonneRepository personnes;

    ConversionEntretien(InterviewerRepository interviewers, EvaluationRepository evaluations,
            DossierRepository dossiers, OffreRepository offres, DepartementOffre departementOffre,
            PosteRepository postes, DepartementRepository departements, PersonneRepository personnes) {
        this.interviewers = interviewers;
        this.evaluations = evaluations;
        this.dossiers = dossiers;
        this.offres = offres;
        this.departementOffre = departementOffre;
        this.postes = postes;
        this.departements = departements;
        this.personnes = personnes;
    }

    List<EntretienResponse> enReponses(List<Entretien> liste, Utilisateur moi) {
        return liste.stream().map(e -> enReponse(e, moi)).toList();
    }

    EntretienResponse enReponse(Entretien e, Utilisateur moi) {
        Dossier d = dossiers.findById(e.getIdDossier()).orElse(null);
        Offre o = d == null ? null : offres.findById(d.getIdOffre()).orElse(null);
        Integer idOffre = o == null ? null : o.getId();
        String poste = o == null ? null : o.getIntitule();
        Integer idDepartement = o == null ? null : departementOffre.idDe(o);
        Integer idPersonne = d == null ? null : d.getIdPersonne();
        String candidat = d == null ? null
                : personnes.findById(d.getIdPersonne()).map(Personne::getNomComplet).orElse(null);

        List<Integer> postesInterviewers = interviewers.findByIdEntretienOrderByIdAsc(e.getId()).stream()
                .map(Interviewer::getIdPoste).toList();
        List<Evaluation> evals = evaluations.findByIdEntretienOrderByDateAscIdAsc(e.getId());

        Integer monPoste = postesInterviewers.contains(moi.getIdPoste()) ? moi.getIdPoste() : null;
        Evaluation mienne = monPoste == null ? null
                : evals.stream().filter(v -> v.getIdPoste().equals(monPoste)).findFirst().orElse(null);
        boolean notesCachees = monPoste != null && mienne == null;
        Integer note = notesCachees || evals.isEmpty() ? null
                : (int) Math.round(evals.stream().mapToInt(Evaluation::getNote).average().orElse(0));
        String raisonEvaluer = raisonEvaluer(e, monPoste, mienne);
        boolean peutEvaluer = raisonEvaluer == null;
        Integer maNote = mienne == null ? null : mienne.getNote();
        Integer idEvaluation = mienne == null ? null : mienne.getId();

        return new EntretienResponse(e.getId(), e.getDate(), e.getIdDossier(), idOffre, poste,
                idDepartement, libelleDepartement(idDepartement), idPersonne, candidat,
                e.getStatut(), e.getStatut().getLibelle(), e.getStatut() == StatutEntretien.FINI,
                postesInterviewers.stream().map(this::interviewer).toList(),
                evals.stream().map(v -> evaluation(v, notesCachees)).toList(),
                note, notesCachees, monPoste, peutEvaluer, raisonEvaluer, maNote, idEvaluation);
    }

    /** Pourquoi je ne peux pas évaluer cet entretien (null si je peux). */
    static String raisonEvaluer(Entretien e, Integer monPoste, Evaluation mienne) {
        if (monPoste == null) return AccesEntretien.PAS_INTERVIEWER;
        if (mienne != null) return AccesEntretien.DEJA_EVALUE;
        if (e.getDate().isAfter(LocalDateTime.now())) return AccesEntretien.A_VENIR;
        return null;
    }

    /** Poste d'un interviewer : libellé, département, détenteur et login du compte qui le tient. */
    InterviewerResponse interviewer(Integer idPoste) {
        Poste p = postes.findById(idPoste).orElse(null);
        Integer idDepartement = p == null ? null : p.getIdDepartement();
        return new InterviewerResponse(idPoste, p == null ? null : p.getLabel(), idDepartement,
                libelleDepartement(idDepartement),
                personnes.detenteurDuPoste(idPoste).map(Personne::getNomComplet).orElse(null),
                interviewers.loginDuPoste(idPoste).orElse(null));
    }

    /** Une évaluation avec son évaluateur ; cachee : sans note ni remarques (notes séparées). */
    EvaluationResponse evaluation(Evaluation v, boolean cachee) {
        InterviewerResponse i = interviewer(v.getIdPoste());
        return new EvaluationResponse(v.getId(), v.getIdPoste(), i.poste(), i.departement(), i.detenteur(),
                i.login(), v.getDate(), cachee ? null : v.getNote(), cachee ? null : v.getRemarques());
    }

    private String libelleDepartement(Integer idDepartement) {
        return idDepartement == null ? null
                : departements.findById(idDepartement).map(Departement::getLabel).orElse(null);
    }
}
