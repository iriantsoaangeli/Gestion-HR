package gestion.hr.demande.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.demande.Dto.CreerDemandeRequest;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Service.DemandeService;

/**
 * Demandes de personnel (aucune règle ici : tout est dans DemandeService).
 * POST /api/demandes                 créer (MANAGER)                       → 201
 * POST /api/demandes/{id}/valider    valider à son étape (HR, DIRECTION, FINANCE)
 * POST /api/demandes/{id}/refuser    refuser à son étape
 * GET  /api/demandes/mes-demandes    demandes que j'ai créées
 * GET  /api/demandes/traitees        demandes sur lesquelles j'ai décidé
 * GET  /api/demandes/a-traiter       demandes qui attendent ma décision
 * GET  /api/demandes                 toutes (ADMIN, DIRECTION)
 * GET  /api/demandes/{id}            une demande (si j'y ai accès)
 * GET  /api/demandes/etapes          étapes du workflow (progression du formulaire)
 */
@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService service;

    public DemandeController(DemandeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeResponse creer(@RequestBody CreerDemandeRequest demande) {
        return service.creer(demande);
    }

    @PostMapping("/{id}/valider")
    public DemandeResponse valider(@PathVariable Integer id) {
        return service.valider(id);
    }

    @PostMapping("/{id}/refuser")
    public DemandeResponse refuser(@PathVariable Integer id) {
        return service.refuser(id);
    }

    @GetMapping("/mes-demandes")
    public List<DemandeResponse> mesDemandes() {
        return service.mesDemandes();
    }

    @GetMapping("/traitees")
    public List<DemandeResponse> traitees() {
        return service.traitees();
    }

    @GetMapping("/a-traiter")
    public List<DemandeResponse> aTraiter() {
        return service.aTraiter();
    }

    @GetMapping
    public List<DemandeResponse> toutes() {
        return service.toutes();
    }

    @GetMapping("/{id:\\d+}")
    public DemandeResponse detail(@PathVariable Integer id) {
        return service.detail(id);
    }

    @GetMapping("/etapes")
    public List<DemandeResponse.Etape> etapes() {
        return service.etapes();
    }
}
