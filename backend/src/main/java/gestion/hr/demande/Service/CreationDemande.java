package gestion.hr.demande.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.demande.Dto.CreerDemandeRequest;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Model.Demande;
import gestion.hr.demande.Model.DemandeFonction;
import gestion.hr.demande.Model.DemandePoste;
import gestion.hr.demande.Repository.DemandeFonctionRepository;
import gestion.hr.demande.Repository.DemandePosteRepository;
import gestion.hr.demande.Repository.DemandeRepository;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.FonctionRepository;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Créer une demande (appelé seulement par DemandeService, qui porte la transaction).
 * - Seul un MANAGER crée une demande ; elle démarre EN_ATTENTE_HR.
 * - Uniquement pour son département (celui de son poste).
 * - Il peut demander son propre remplacement, pas celui d'un poste tenu par un autre compte (managers, supérieurs).
 */
@Component
class CreationDemande {

    /** Le délai doit laisser au moins une semaine. */
    static final int DELAI_MINIMUM_JOURS = 7;
    static final int MOTIF_MAX = 200;

    private final DemandeRepository demandes;
    private final DemandeFonctionRepository creations;
    private final DemandePosteRepository remplacements;
    private final DepartementRepository departements;
    private final FonctionRepository fonctions;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurCourant courant;
    private final ConversionDemande conversion;

    CreationDemande(DemandeRepository demandes, DemandeFonctionRepository creations,
            DemandePosteRepository remplacements, DepartementRepository departements, FonctionRepository fonctions,
            PosteRepository postes, UtilisateurRepository utilisateurs, UtilisateurCourant courant,
            ConversionDemande conversion) {
        this.demandes = demandes;
        this.creations = creations;
        this.remplacements = remplacements;
        this.departements = departements;
        this.fonctions = fonctions;
        this.postes = postes;
        this.utilisateurs = utilisateurs;
        this.courant = courant;
        this.conversion = conversion;
    }

    DemandeResponse creer(CreerDemandeRequest req) {
        Utilisateur moi = courant.get();
        if (!moi.getRole().peutCreerDemande()) {
            throw ErreurMetier.accesRefuse("Seul un manager peut créer une demande.");
        }
        verifier(req, moi);

        Demande d = demandes.save(new Demande(req.idDepartement(), moi.getId(), req.motif().trim(), req.delai()));
        for (CreerDemandeRequest.Creation c : liste(req.creations())) {
            creations.save(new DemandeFonction(d.getId(), c.idFonction(), c.effectif()));
        }
        for (Integer idPoste : liste(req.remplacements())) {
            remplacements.save(new DemandePoste(d.getId(), idPoste));
        }
        return conversion.enReponse(d, moi);
    }

    private void verifier(CreerDemandeRequest req, Utilisateur moi) {
        if (req.idDepartement() == null || !departements.existsById(req.idDepartement())) {
            throw ErreurMetier.validation("Choisissez un département.");
        }
        if (!req.idDepartement().equals(postes.departementDe(moi.getId()).orElse(null))) {
            throw ErreurMetier.accesRefuse("Vous ne pouvez faire une demande que pour votre département.");
        }
        if (req.motif() == null || req.motif().isBlank()) {
            throw ErreurMetier.validation("Le motif est obligatoire.");
        }
        if (req.motif().trim().length() > MOTIF_MAX) {
            throw ErreurMetier.validation("Le motif ne doit pas dépasser " + MOTIF_MAX + " caractères.");
        }
        if (req.delai() != null && req.delai().isBefore(LocalDate.now().plusDays(DELAI_MINIMUM_JOURS))) {
            throw ErreurMetier.validation("Le délai doit être d'au moins une semaine.");
        }

        List<CreerDemandeRequest.Creation> aCreer = liste(req.creations());
        List<Integer> aRemplacer = liste(req.remplacements());
        if (aCreer.isEmpty() && aRemplacer.isEmpty()) {
            throw ErreurMetier.validation("Ajoutez ou remplacez au moins un poste.");
        }

        Set<Integer> vues = new HashSet<>();
        for (CreerDemandeRequest.Creation c : aCreer) {
            if (c == null || c.idFonction() == null || !fonctions.existsById(c.idFonction())) {
                throw ErreurMetier.validation("Fonction inconnue.");
            }
            if (c.effectif() == null || c.effectif() < 1) {
                throw ErreurMetier.validation("L'effectif d'une création doit être d'au moins 1.");
            }
            if (!vues.add(c.idFonction())) {
                throw ErreurMetier.validation("Une même fonction ne peut être demandée qu'une fois (augmentez l'effectif).");
            }
        }

        vues.clear();
        for (Integer idPoste : aRemplacer) {
            Poste p = idPoste == null ? null : postes.findById(idPoste).orElse(null);
            if (p == null) {
                throw ErreurMetier.validation("Poste à remplacer inconnu.");
            }
            if (!p.getIdDepartement().equals(req.idDepartement())) {
                throw ErreurMetier.validation("Le poste « " + p.getLabel() + " » n'est pas dans ce département.");
            }
            if (!vues.add(idPoste)) {
                throw ErreurMetier.validation("Le poste « " + p.getLabel() + " » est demandé deux fois.");
            }
            if (utilisateurs.existsByIdPosteAndIdNot(idPoste, moi.getId())) {
                throw ErreurMetier.accesRefuse("Le poste « " + p.getLabel()
                        + " » est tenu par un autre compte (manager ou supérieur) : vous ne pouvez pas demander son remplacement.");
            }
        }
    }

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l;
    }
}
