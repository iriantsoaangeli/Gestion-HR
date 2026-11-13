package gestion.hr.offre.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.offre.Dto.DemandeApprouveeResponse;
import gestion.hr.offre.Dto.OffreResponse;
import gestion.hr.offre.Dto.PublierOffreRequest;
import gestion.hr.offre.Service.OffreService;

/**
 * Offres d'emploi (aucune règle ici : tout est dans OffreService). Contrat : doc/routes/api/offres.md.
 * GET  /api/offres                       toutes les offres (HR)
 * GET  /api/offres/{id}                  une offre (HR)
 * GET  /api/offres/demandes-approuvees   demandes approuvées, lignes déjà couvertes indiquées (HR)
 * POST /api/offres                       publier une offre depuis une ligne d'une demande approuvée (HR) → 201
 */
@RestController
@RequestMapping("/api/offres")
public class OffreController {

    private final OffreService service;

    public OffreController(OffreService service) {
        this.service = service;
    }

    @GetMapping
    public List<OffreResponse> offres() {
        return service.offres();
    }

    @GetMapping("/{id:\\d+}")
    public OffreResponse offre(@PathVariable Integer id) {
        return service.offre(id);
    }

    @GetMapping("/demandes-approuvees")
    public List<DemandeApprouveeResponse> demandesApprouvees() {
        return service.demandesApprouvees();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OffreResponse publier(@RequestBody PublierOffreRequest offre) {
        return service.publier(offre);
    }
}
