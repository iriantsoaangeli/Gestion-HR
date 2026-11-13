package gestion.hr.entretien.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.EvaluerRequest;
import gestion.hr.entretien.Dto.InterviewerResponse;
import gestion.hr.entretien.Dto.PreparerEntretienRequest;
import gestion.hr.entretien.Service.EntretienService;

/**
 * Entretiens de recrutement (aucune règle ici : tout est dans EntretienService). Contrat : doc/routes/api/entretiens.md
 * POST /api/entretiens                        préparer (HR)                                    → 201
 * POST /api/entretiens/{id}/evaluation        évaluer (l'interviewer connecté, une seule fois) → 201
 * GET  /api/entretiens/en-attente             entretiens en attente (MANAGER, HR, DIRECTION)
 * GET  /api/entretiens/finis                  entretiens finis
 * GET  /api/entretiens?offre={id}             tous les entretiens, ou ceux d'une offre
 * GET  /api/entretiens/{id}                   un entretien
 * GET  /api/entretiens/interviewers-possibles postes qui peuvent faire un entretien (HR)
 */
@RestController
@RequestMapping("/api/entretiens")
public class EntretienController {

    private final EntretienService service;

    public EntretienController(EntretienService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntretienResponse preparer(@RequestBody PreparerEntretienRequest entretien) {
        return service.preparer(entretien);
    }

    @PostMapping("/{id:\\d+}/evaluation")
    @ResponseStatus(HttpStatus.CREATED)
    public EntretienResponse evaluer(@PathVariable Integer id, @RequestBody EvaluerRequest evaluation) {
        return service.evaluer(id, evaluation);
    }

    @GetMapping("/en-attente")
    public List<EntretienResponse> enAttente() {
        return service.enAttente();
    }

    @GetMapping("/finis")
    public List<EntretienResponse> finis() {
        return service.finis();
    }

    @GetMapping
    public List<EntretienResponse> liste(@RequestParam(name = "offre", required = false) Integer idOffre) {
        return service.liste(idOffre);
    }

    @GetMapping("/{id:\\d+}")
    public EntretienResponse detail(@PathVariable Integer id) {
        return service.detail(id);
    }

    @GetMapping("/interviewers-possibles")
    public List<InterviewerResponse> interviewersPossibles() {
        return service.interviewersPossibles();
    }
}
