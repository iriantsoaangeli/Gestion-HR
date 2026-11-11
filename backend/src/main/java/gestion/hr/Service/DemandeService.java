package gestion.hr.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gestion.hr.Dto.CreerDemandeRequest;
import gestion.hr.Dto.DemandeResponse;
import gestion.hr.Exception.ErreurMetier;
import gestion.hr.Model.Demande;
import gestion.hr.Model.DemandeFonction;
import gestion.hr.Model.DemandePoste;
import gestion.hr.Model.HistoDemande;
import gestion.hr.Model.Poste;
import gestion.hr.Model.Statut;
import gestion.hr.Repository.DemandeFonctionRepository;
import gestion.hr.Repository.DemandePosteRepository;
import gestion.hr.Repository.DemandeRepository;
import gestion.hr.Repository.DepartementRepository;
import gestion.hr.Repository.FonctionRepository;
import gestion.hr.Repository.HistoDemandeRepository;
import gestion.hr.Repository.PosteRepository;
import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.auth.Service.UtilisateurCourant;

/**
 * Règles des demandes de personnel (workflow Manager -> HR -> Direction -> Finance).
 * - Seul un MANAGER crée une demande ; elle démarre EN_ATTENTE_HR.
 * - À chaque étape, seul le rôle de l'étape (Statut.getValideur) valide ou refuse.
 * - Chaque décision est écrite dans histoDemande, dans la même transaction que le changement de statut.
 * - Une demande n'est visible que par son demandeur, ceux qui ont décidé dessus, le rôle dont c'est le tour,
 *   et ADMIN / DIRECTION (toutes les demandes).
 */
@Service
public class DemandeService {

    /** Le délai doit laisser au moins une semaine. */
    static final int DELAI_MINIMUM_JOURS = 7;
    static final int MOTIF_MAX = 200;

    private final DemandeRepository demandes;
    private final DemandeFonctionRepository creations;
    private final DemandePosteRepository remplacements;
    private final HistoDemandeRepository historique;
    private final DepartementRepository departements;
    private final FonctionRepository fonctions;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurCourant courant;

    public DemandeService(DemandeRepository demandes, DemandeFonctionRepository creations,
            DemandePosteRepository remplacements, HistoDemandeRepository historique,
            DepartementRepository departements, FonctionRepository fonctions, PosteRepository postes,
            UtilisateurRepository utilisateurs, UtilisateurCourant courant) {
        this.demandes = demandes;
        this.creations = creations;
        this.remplacements = remplacements;
        this.historique = historique;
        this.departements = departements;
        this.fonctions = fonctions;
        this.postes = postes;
        this.utilisateurs = utilisateurs;
        this.courant = courant;
    }

    // ---------- Créer ----------

    @Transactional
    public DemandeResponse creer(CreerDemandeRequest req) {
        Utilisateur moi = courant.get();
        if (!moi.getRole().peutCreerDemande()) {
            throw ErreurMetier.accesRefuse("Seul un manager peut créer une demande.");
        }
        verifier(req);

        Demande d = demandes.save(new Demande(req.idDepartement(), moi.getId(), req.motif().trim(), req.delai()));
        for (CreerDemandeRequest.Creation c : liste(req.creations())) {
            creations.save(new DemandeFonction(d.getId(), c.idFonction(), c.effectif()));
        }
        for (Integer idPoste : liste(req.remplacements())) {
            remplacements.save(new DemandePoste(d.getId(), idPoste));
        }
        return enReponse(d, moi);
    }

    private void verifier(CreerDemandeRequest req) {
        if (req.idDepartement() == null || !departements.existsById(req.idDepartement())) {
            throw ErreurMetier.validation("Choisissez un département.");
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
        }
    }

    // ---------- Valider / refuser ----------

    @Transactional
    public DemandeResponse valider(Integer id) {
        return decider(id, true);
    }

    @Transactional
    public DemandeResponse refuser(Integer id) {
        return decider(id, false);
    }

    private DemandeResponse decider(Integer id, boolean valide) {
        Utilisateur moi = courant.get();
        Demande d = demandes.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("La demande n° " + id + " n'existe pas."));

        Statut actuel = d.getStatut();
        if (!actuel.estEnAttente()) {
            throw new ErreurMetier(HttpStatus.CONFLICT, "TRANSITION_INTERDITE",
                    "La demande n° " + id + " est déjà traitée (" + actuel.getLibelle() + ").");
        }
        if (actuel.getValideur() != moi.getRole()) {
            throw ErreurMetier.accesRefuse("Cette demande attend la décision de l'étape "
                    + actuel.getValideur().getLibelle() + ".");
        }

        Statut nouveau = valide ? actuel.apresValidation() : Statut.REFUSEE;
        d.setStatut(nouveau);
        historique.save(new HistoDemande(d.getId(), moi.getId(), nouveau));
        return enReponse(d, moi);
    }

    // ---------- Lire ----------

    /** Une demande, si je peux la voir (sinon 403). */
    @Transactional(readOnly = true)
    public DemandeResponse detail(Integer id) {
        Utilisateur moi = courant.get();
        Demande d = demandes.findById(id)
                .orElseThrow(() -> ErreurMetier.introuvable("La demande n° " + id + " n'existe pas."));
        boolean visible = d.getIdDemandeur().equals(moi.getId())
                || moi.getRole().voitToutesLesDemandes()
                || d.getStatut().getValideur() == moi.getRole()
                || historique.existsByIdDemandeAndIdUtilisateur(d.getId(), moi.getId());
        if (!visible) {
            throw ErreurMetier.accesRefuse("Vous n'avez pas accès à la demande n° " + id + ".");
        }
        return enReponse(d, moi);
    }

    /** Toutes les demandes : ADMIN et DIRECTION seulement. */
    @Transactional(readOnly = true)
    public List<DemandeResponse> toutes() {
        Utilisateur moi = courant.get();
        if (!moi.getRole().voitToutesLesDemandes()) {
            throw ErreurMetier.accesRefuse("Seules l'administration et la direction voient toutes les demandes.");
        }
        return enReponses(demandes.findAllByOrderByDateDescIdDesc(), moi);
    }

    /** Demandes que j'ai créées. */
    @Transactional(readOnly = true)
    public List<DemandeResponse> mesDemandes() {
        Utilisateur moi = courant.get();
        return enReponses(demandes.findByIdDemandeurOrderByDateDescIdDesc(moi.getId()), moi);
    }

    /** Demandes qui attendent ma décision (les plus anciennes d'abord). Vide si mon rôle ne valide pas. */
    @Transactional(readOnly = true)
    public List<DemandeResponse> aTraiter() {
        Utilisateur moi = courant.get();
        List<Statut> statuts = Statut.enAttenteDe(moi.getRole());
        return statuts.isEmpty() ? List.of() : enReponses(demandes.findByStatutInOrderByDateAscIdAsc(statuts), moi);
    }

    /** Demandes sur lesquelles j'ai pris une décision (validée ou refusée). */
    @Transactional(readOnly = true)
    public List<DemandeResponse> traitees() {
        Utilisateur moi = courant.get();
        return enReponses(demandes.findTraiteesPar(moi.getId()), moi);
    }

    /** Étapes du workflow pour une demande pas encore créée (progression du formulaire). */
    public List<DemandeResponse.Etape> etapes() {
        List<DemandeResponse.Etape> etapes = new ArrayList<>();
        etapes.add(new DemandeResponse.Etape(Role.MANAGER.getLibelle(), "courante", "Création de la demande", null, null));
        for (Statut s : Statut.etapesDeValidation()) {
            etapes.add(new DemandeResponse.Etape(s.getValideur().getLibelle(), "a-venir", "À venir", null, null));
        }
        return etapes;
    }

    // ---------- Conversion ----------

    private List<DemandeResponse> enReponses(List<Demande> liste, Utilisateur moi) {
        return liste.stream().map(d -> enReponse(d, moi)).toList();
    }

    private DemandeResponse enReponse(Demande d, Utilisateur moi) {
        String departement = departements.findById(d.getIdDepartement()).map(x -> x.getLabel()).orElse(null);
        String demandeur = utilisateurs.findById(d.getIdDemandeur()).map(Utilisateur::getLogin).orElse(null);

        List<DemandeResponse.Creation> c = creations.findByIdDemande(d.getId()).stream()
                .map(x -> new DemandeResponse.Creation(x.getIdFonction(),
                        fonctions.findById(x.getIdFonction()).map(f -> f.getLabel()).orElse(null), x.getEffectif()))
                .toList();
        List<DemandeResponse.Remplacement> r = remplacements.findByIdDemande(d.getId()).stream()
                .map(x -> new DemandeResponse.Remplacement(x.getIdPoste(),
                        postes.findById(x.getIdPoste()).map(Poste::getLabel).orElse(null)))
                .toList();
        List<DemandeResponse.Decision> h = historique.findByIdDemandeOrderByDateAscIdAsc(d.getId()).stream()
                .map(x -> {
                    Utilisateur u = utilisateurs.findById(x.getIdUtilisateur()).orElse(null);
                    return new DemandeResponse.Decision(u == null ? null : u.getLogin(),
                            u == null ? null : u.getRole().getLibelle(), x.getStatut(), x.getDate());
                })
                .toList();

        Role valideur = d.getStatut().getValideur();
        return new DemandeResponse(d.getId(), d.getIdDepartement(), departement, d.getStatut(),
                d.getStatut().getLibelle(), valideur == null ? null : valideur.name(), d.getIdDemandeur(), demandeur,
                d.getDate(), d.getMotif(), d.getDelai(), c, r, h,
                valideur != null && valideur == moi.getRole(), suivi(d, demandeur, h));
    }

    /**
     * Une ligne par étape : création, puis chaque étape de validation. Les décisions arrivent dans l'ordre
     * du workflow, donc la k-ième décision correspond à la k-ième étape de validation.
     */
    private List<DemandeResponse.Etape> suivi(Demande d, String demandeur, List<DemandeResponse.Decision> h) {
        List<DemandeResponse.Etape> suivi = new ArrayList<>();
        suivi.add(new DemandeResponse.Etape(Role.MANAGER.getLibelle(), "faite", "Demande créée", d.getDate(), demandeur));

        boolean refusee = d.getStatut() == Statut.REFUSEE;
        LocalDate derniereDate = h.isEmpty() ? d.getDate() : h.get(h.size() - 1).date();
        List<Statut> etapes = Statut.etapesDeValidation();
        for (int k = 0; k < etapes.size(); k++) {
            String libelle = etapes.get(k).getValideur().getLibelle();
            if (k < h.size()) {
                DemandeResponse.Decision dec = h.get(k);
                boolean refus = dec.statut() == Statut.REFUSEE;
                suivi.add(new DemandeResponse.Etape(libelle, refus ? "refusee" : "faite", refus ? "Refusée" : "Validée",
                        dec.date(), dec.utilisateur()));
            } else if (etapes.get(k) == d.getStatut()) {
                suivi.add(new DemandeResponse.Etape(libelle, "courante", "En attente depuis", derniereDate, null));
            } else {
                suivi.add(new DemandeResponse.Etape(libelle, "a-venir", refusee ? "Non atteinte" : "À venir", null, null));
            }
        }
        return suivi;
    }

    private static <T> List<T> liste(List<T> l) {
        return l == null ? List.of() : l;
    }
}
