package gestion.hr.entretien.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.dossier.Model.Dossier;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.entretien.Dto.EntretienResponse;
import gestion.hr.entretien.Dto.PreparerEntretienRequest;
import gestion.hr.entretien.Model.Entretien;
import gestion.hr.entretien.Model.Interviewer;
import gestion.hr.entretien.Repository.EntretienRepository;
import gestion.hr.entretien.Repository.InterviewerRepository;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Repository.OffreRepository;

/**
 * Préparer un entretien (HR ; appelé seulement par EntretienService, qui porte la transaction).
 * - Un dossier existant d'une offre ouverte ; un candidat peut avoir plusieurs entretiens pour la même offre.
 * - Date-heure dans le futur.
 * - Au moins un interviewer, chacun parmi AccesEntretien.postesPossibles(), sans doublon.
 * L'entretien démarre EN_ATTENTE.
 */
@Component
class PreparationEntretien {

    private final EntretienRepository entretiens;
    private final InterviewerRepository interviewers;
    private final DossierRepository dossiers;
    private final OffreRepository offres;
    private final AccesEntretien acces;
    private final ConversionEntretien conversion;

    PreparationEntretien(EntretienRepository entretiens, InterviewerRepository interviewers,
            DossierRepository dossiers, OffreRepository offres, AccesEntretien acces,
            ConversionEntretien conversion) {
        this.entretiens = entretiens;
        this.interviewers = interviewers;
        this.dossiers = dossiers;
        this.offres = offres;
        this.acces = acces;
        this.conversion = conversion;
    }

    EntretienResponse preparer(PreparerEntretienRequest req) {
        Utilisateur moi = acces.exigerHr();

        if (req.idDossier() == null) {
            throw ErreurMetier.validation("Choisissez un candidat (un dossier déposé pour une offre).");
        }
        Dossier d = dossiers.findById(req.idDossier())
                .orElseThrow(() -> ErreurMetier.validation("Le dossier n° " + req.idDossier() + " n'existe pas."));
        Offre o = offres.findById(d.getIdOffre())
                .orElseThrow(() -> ErreurMetier.validation("L'offre de ce dossier n'existe pas."));
        if (!o.estOuverte()) {
            throw ErreurMetier.validation("L'offre « " + o.getIntitule() + " » est close : plus d'entretien à préparer.");
        }

        if (req.date() == null) {
            throw ErreurMetier.validation("Indiquez la date et l'heure de l'entretien.");
        }
        if (!req.date().isAfter(LocalDateTime.now())) {
            throw ErreurMetier.validation("La date de l'entretien doit être dans le futur.");
        }

        List<Integer> choisis = req.interviewers() == null ? List.of() : req.interviewers();
        if (choisis.isEmpty()) {
            throw ErreurMetier.validation("Choisissez au moins une personne qui fait l'entretien.");
        }
        Set<Integer> possibles = new HashSet<>(acces.postesPossibles());
        Set<Integer> vus = new HashSet<>();
        for (Integer idPoste : choisis) {
            if (idPoste == null || !possibles.contains(idPoste)) {
                throw ErreurMetier.validation("Interviewer non autorisé : choisissez le poste d'un compte actif ("
                        + AccesEntretien.libelles() + ").");
            }
            if (!vus.add(idPoste)) {
                throw ErreurMetier.validation("Un même interviewer est choisi deux fois.");
            }
        }

        Entretien e = entretiens.save(new Entretien(req.date().withNano(0), d.getId()));
        for (Integer idPoste : choisis) {
            interviewers.save(new Interviewer(e.getId(), idPoste));
        }
        return conversion.enReponse(e, moi);
    }
}
