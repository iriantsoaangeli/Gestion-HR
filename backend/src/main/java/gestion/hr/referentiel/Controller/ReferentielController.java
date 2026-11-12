package gestion.hr.referentiel.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.referentiel.Dto.ReferentielResponse;
import gestion.hr.referentiel.Service.ReferentielService;

/**
 * Listes de choix pour les formulaires (lecture seule, tout utilisateur connecté ; règles dans ReferentielService).
 * GET /api/referentiel/departements
 * GET /api/referentiel/mon-departement             (département du compte connecté = celui de son poste)
 * GET /api/referentiel/fonctions                   (création de postes)
 * GET /api/referentiel/postes?departement={id}     (postes que je peux faire remplacer)
 */
@RestController
@RequestMapping("/api/referentiel")
public class ReferentielController {

    private final ReferentielService service;

    public ReferentielController(ReferentielService service) {
        this.service = service;
    }

    @GetMapping("/departements")
    public List<ReferentielResponse> departements() {
        return service.departements();
    }

    @GetMapping("/mon-departement")
    public ReferentielResponse monDepartement() {
        return service.monDepartement();
    }

    @GetMapping("/fonctions")
    public List<ReferentielResponse> fonctions() {
        return service.fonctions();
    }

    @GetMapping("/postes")
    public List<ReferentielResponse> postes(@RequestParam("departement") Integer idDepartement) {
        return service.postesRemplacables(idDepartement);
    }
}
