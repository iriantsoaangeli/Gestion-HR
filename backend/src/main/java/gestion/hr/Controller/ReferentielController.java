package gestion.hr.Controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gestion.hr.Dto.ReferentielResponse;
import gestion.hr.Repository.DepartementRepository;
import gestion.hr.Repository.FonctionRepository;
import gestion.hr.Repository.PosteRepository;

/**
 * Listes de choix pour les formulaires (lecture seule, tout utilisateur connecté).
 * GET /api/referentiel/departements
 * GET /api/referentiel/fonctions                    (création de postes)
 * GET /api/referentiel/postes?departement={id}      (postes existants à remplacer)
 */
@RestController
@RequestMapping("/api/referentiel")
public class ReferentielController {

    private final DepartementRepository departements;
    private final FonctionRepository fonctions;
    private final PosteRepository postes;

    public ReferentielController(DepartementRepository departements, FonctionRepository fonctions,
            PosteRepository postes) {
        this.departements = departements;
        this.fonctions = fonctions;
        this.postes = postes;
    }

    @GetMapping("/departements")
    public List<ReferentielResponse> departements() {
        return departements.findAllByOrderByLabel().stream()
                .map(d -> new ReferentielResponse(d.getId(), d.getLabel())).toList();
    }

    @GetMapping("/fonctions")
    public List<ReferentielResponse> fonctions() {
        return fonctions.findAllByOrderByLabel().stream()
                .map(f -> new ReferentielResponse(f.getId(), f.getLabel())).toList();
    }

    @GetMapping("/postes")
    public List<ReferentielResponse> postes(@RequestParam("departement") Integer idDepartement) {
        return postes.findByIdDepartementOrderByLabel(idDepartement).stream()
                .map(p -> new ReferentielResponse(p.getId(), p.getLabel())).toList();
    }
}
