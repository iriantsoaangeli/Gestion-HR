package gestion.hr.entretien.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.FicheEvaluationResponse;
import gestion.hr.entretien.Service.EntretienService;

/**
 * Mes évaluations d'entretien (aucune règle ici : tout est dans EntretienService). On évalue par
 * POST /api/entretiens/{id}/evaluation (EntretienController). Contrat : doc/routes/api/entretiens.md
 * GET /api/evaluations/a-faire   entretiens dont je suis interviewer, pas encore évalués par moi
 * GET /api/evaluations/faites    entretiens que j'ai évalués (maNote, idEvaluation)
 * GET /api/evaluations/{id}      fiche d'une évaluation (refusée tant que les notes me sont cachées)
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EntretienService service;

    public EvaluationController(EntretienService service) {
        this.service = service;
    }

    @GetMapping("/a-faire")
    public List<EntretienResponse> aFaire() {
        return service.aFaire();
    }

    @GetMapping("/faites")
    public List<EntretienResponse> faites() {
        return service.faites();
    }

    @GetMapping("/{id:\\d+}")
    public FicheEvaluationResponse fiche(@PathVariable Integer id) {
        return service.fiche(id);
    }
}
