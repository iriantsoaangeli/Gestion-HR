package gestion.hr.dossier.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import gestion.hr.dossier.Dto.DeposerDossierRequest;
import gestion.hr.dossier.Dto.DossierResponse;
import gestion.hr.dossier.Dto.FichierResponse;
import gestion.hr.dossier.Dto.PersonneResponse;

/**
 * Point d'entrée unique des dossiers de candidature (HR seulement : AccesDossier).
 * Chaque section est dans sa classe du même package, invisible hors de celui-ci :
 * - DepotDossier   : déposer un dossier (offre ouverte, candidat existant ou nouveau, pièces jointes ≤ 30 Mo)
 * - LectureDossier : personnes candidates, dossiers avec candidat et pièces jointes, téléchargement
 * - AccesDossier   : le droit (rôle HR)
 * Les transactions sont ouvertes ici.
 */
@Service
public class DossierService {

    private final DepotDossier depot;
    private final LectureDossier lecture;
    private final AccesDossier acces;

    DossierService(DepotDossier depot, LectureDossier lecture, AccesDossier acces) {
        this.depot = depot;
        this.lecture = lecture;
        this.acces = acces;
    }

    @Transactional
    public DossierResponse deposer(DeposerDossierRequest req, List<MultipartFile> fichiers) {
        return depot.deposer(req, fichiers);
    }

    @Transactional(readOnly = true)
    public List<PersonneResponse> personnes() {
        acces.exigerHr();
        return lecture.personnes();
    }

    /** idOffre null = tous les dossiers. */
    @Transactional(readOnly = true)
    public List<DossierResponse> dossiers(Integer idOffre) {
        acces.exigerHr();
        return lecture.dossiers(idOffre);
    }

    @Transactional(readOnly = true)
    public FichierResponse pieceJointe(Integer id) {
        acces.exigerHr();
        return lecture.fichier(id);
    }
}
