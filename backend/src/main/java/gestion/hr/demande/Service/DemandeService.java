package gestion.hr.demande.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gestion.hr.demande.Dto.CreerDemandeRequest;
import gestion.hr.demande.Dto.DemandeResponse;

/**
 * Point d'entrée unique des demandes de personnel (workflow Manager -> HR -> Direction -> Finance).
 * Chaque section est dans sa classe du même package, invisible hors de celui-ci :
 * - CreationDemande  : créer (MANAGER, son département, pas les postes des autres comptes)
 * - DecisionDemande  : valider / refuser à son étape, avec l'historique
 * - LectureDemande   : fiche, listes, validateurs, étapes (qui voit quoi)
 * - ConversionDemande : Demande -> DemandeResponse (libellés, historique, suivi)
 * Les transactions sont ouvertes ici.
 */
@Service
public class DemandeService {

    private final CreationDemande creation;
    private final DecisionDemande decision;
    private final LectureDemande lecture;

    DemandeService(CreationDemande creation, DecisionDemande decision, LectureDemande lecture) {
        this.creation = creation;
        this.decision = decision;
        this.lecture = lecture;
    }

    // ---------- Créer ----------

    @Transactional
    public DemandeResponse creer(CreerDemandeRequest req) {
        return creation.creer(req);
    }

    // ---------- Valider / refuser ----------

    @Transactional
    public DemandeResponse valider(Integer id) {
        return decision.decider(id, true);
    }

    @Transactional
    public DemandeResponse refuser(Integer id) {
        return decision.decider(id, false);
    }

    // ---------- Lire ----------

    @Transactional(readOnly = true)
    public DemandeResponse detail(Integer id) {
        return lecture.detail(id);
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse.Validation> validateurs(Integer id) {
        return lecture.validateurs(id);
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> toutes() {
        return lecture.toutes();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> mesDemandes() {
        return lecture.mesDemandes();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> aTraiter() {
        return lecture.aTraiter();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> traitees() {
        return lecture.traitees();
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse.Etape> etapes() {
        return lecture.etapes();
    }
}
