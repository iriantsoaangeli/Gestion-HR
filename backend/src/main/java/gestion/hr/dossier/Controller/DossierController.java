package gestion.hr.dossier.Controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gestion.hr.dossier.Dto.DeposerDossierRequest;
import gestion.hr.dossier.Dto.DossierResponse;
import gestion.hr.dossier.Dto.FichierResponse;
import gestion.hr.dossier.Dto.PersonneResponse;
import gestion.hr.dossier.Service.DossierService;

/**
 * Dossiers de candidature, HR seulement (aucune règle ici : tout est dans DossierService).
 * POST /api/dossiers                      déposer (multipart/form-data : champs + parts « fichiers »)  → 201
 * GET  /api/dossiers                      tous les dossiers ; ?offre={id} : ceux d'une offre
 * GET  /api/dossiers/personnes            personnes de la base (candidats possibles), employé = a un poste
 * GET  /api/dossiers/pieces-jointes/{id}  télécharger une pièce jointe (Content-Type et nom du fichier d'origine)
 * Contrat : doc/routes/api/dossiers.md.
 */
@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private final DossierService service;

    public DossierController(DossierService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DossierResponse deposer(@ModelAttribute DeposerDossierRequest dossier,
            @RequestParam(name = "fichiers", required = false) List<MultipartFile> fichiers) {
        return service.deposer(dossier, fichiers);
    }

    @GetMapping
    public List<DossierResponse> dossiers(@RequestParam(name = "offre", required = false) Integer idOffre) {
        return service.dossiers(idOffre);
    }

    @GetMapping("/personnes")
    public List<PersonneResponse> personnes() {
        return service.personnes();
    }

    @GetMapping("/pieces-jointes/{id:\\d+}")
    public ResponseEntity<byte[]> telecharger(@PathVariable Integer id) {
        FichierResponse f = service.pieceJointe(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .body(f.contenu());
    }
}
