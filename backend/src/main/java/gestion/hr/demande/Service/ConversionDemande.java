package gestion.hr.demande.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.demande.Model.Demande;
import gestion.hr.demande.Model.HistoDemande;
import gestion.hr.demande.Model.Statut;
import gestion.hr.demande.Repository.DemandeFonctionRepository;
import gestion.hr.demande.Repository.DemandePosteRepository;
import gestion.hr.demande.Repository.HistoDemandeRepository;
import gestion.hr.referentiel.Dto.PersonnelResponse;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.FonctionRepository;
import gestion.hr.referentiel.Repository.PosteRepository;
import gestion.hr.referentiel.Service.ReferentielService;

/**
 * Demande -> DemandeResponse, avec les libellés, l'historique et le suivi déjà calculés
 * (utilisé par CreationDemande, DecisionDemande et LectureDemande).
 */
@Component
class ConversionDemande {

    private final DemandeFonctionRepository creations;
    private final DemandePosteRepository remplacements;
    private final HistoDemandeRepository historique;
    private final DepartementRepository departements;
    private final FonctionRepository fonctions;
    private final PosteRepository postes;
    private final UtilisateurRepository utilisateurs;
    private final ReferentielService referentiel;

    ConversionDemande(DemandeFonctionRepository creations, DemandePosteRepository remplacements,
            HistoDemandeRepository historique, DepartementRepository departements, FonctionRepository fonctions,
            PosteRepository postes, UtilisateurRepository utilisateurs, ReferentielService referentiel) {
        this.creations = creations;
        this.remplacements = remplacements;
        this.historique = historique;
        this.departements = departements;
        this.fonctions = fonctions;
        this.postes = postes;
        this.utilisateurs = utilisateurs;
        this.referentiel = referentiel;
    }

    List<DemandeResponse> enReponses(List<Demande> liste, Utilisateur moi) {
        return liste.stream().map(d -> enReponse(d, moi)).toList();
    }

    DemandeResponse enReponse(Demande d, Utilisateur moi) {
        String departement = departements.findById(d.getIdDepartement()).map(x -> x.getLabel()).orElse(null);
        Utilisateur demandeurU = utilisateurs.findById(d.getIdDemandeur()).orElse(null);
        String demandeur = demandeurU == null ? null : demandeurU.getLogin();

        List<DemandeResponse.Creation> c = creations.findByIdDemande(d.getId()).stream()
                .map(x -> new DemandeResponse.Creation(x.getIdFonction(),
                        fonctions.findById(x.getIdFonction()).map(f -> f.getLabel()).orElse(null), x.getEffectif()))
                .toList();
        List<DemandeResponse.Remplacement> r = remplacements.findByIdDemande(d.getId()).stream()
                .map(x -> new DemandeResponse.Remplacement(x.getIdPoste(),
                        postes.findById(x.getIdPoste()).map(Poste::getLabel).orElse(null)))
                .toList();
        // Décisions dans l'ordre, avec le compte de chaque décideur (pour l'historique et le suivi)
        List<HistoDemande> decisions = historique.findByIdDemandeOrderByDateAscIdAsc(d.getId());
        List<Utilisateur> decideurs = decisions.stream()
                .map(x -> utilisateurs.findById(x.getIdUtilisateur()).orElse(null)).toList();
        List<DemandeResponse.Decision> h = new ArrayList<>();
        for (int k = 0; k < decisions.size(); k++) {
            Utilisateur u = decideurs.get(k);
            h.add(new DemandeResponse.Decision(u == null ? null : u.getLogin(),
                    u == null ? null : u.getRole().getLibelle(), decisions.get(k).getStatut(), decisions.get(k).getDate()));
        }

        Role valideur = d.getStatut().getValideur();
        return new DemandeResponse(d.getId(), d.getIdDepartement(), departement, d.getStatut(),
                d.getStatut().getLibelle(), valideur == null ? null : valideur.name(), d.getIdDemandeur(), demandeur,
                d.getDate(), d.getMotif(), d.getDelai(), c, r, h,
                valideur != null && valideur == moi.getRole(), suivi(d, demandeurU, h, decideurs));
    }

    /**
     * Une ligne par étape : création, puis chaque étape de validation. Les décisions arrivent dans l'ordre
     * du workflow, donc la k-ième décision correspond à la k-ième étape de validation.
     * Personnel de l'étape : celui qui a agi si elle est faite, sinon les comptes du rôle qui décidera.
     */
    private List<DemandeResponse.Etape> suivi(Demande d, Utilisateur demandeur, List<DemandeResponse.Decision> h,
            List<Utilisateur> decideurs) {
        List<DemandeResponse.Etape> suivi = new ArrayList<>();
        suivi.add(new DemandeResponse.Etape(Role.MANAGER.getLibelle(), "faite", "Demande créée", d.getDate(),
                demandeur == null ? null : demandeur.getLogin(), seul(referentiel.personnel(demandeur))));

        boolean refusee = d.getStatut() == Statut.REFUSEE;
        LocalDateTime derniereDate = h.isEmpty() ? d.getDate() : h.get(h.size() - 1).date();
        List<Statut> etapes = Statut.etapesDeValidation();
        for (int k = 0; k < etapes.size(); k++) {
            Role role = etapes.get(k).getValideur();
            String libelle = role.getLibelle();
            if (k < h.size()) {
                DemandeResponse.Decision dec = h.get(k);
                boolean refus = dec.statut() == Statut.REFUSEE;
                suivi.add(new DemandeResponse.Etape(libelle, refus ? "refusee" : "faite", refus ? "Refusée" : "Validée",
                        dec.date(), dec.utilisateur(), seul(referentiel.personnel(decideurs.get(k)))));
            } else if (etapes.get(k) == d.getStatut()) {
                suivi.add(new DemandeResponse.Etape(libelle, "courante", "En attente depuis", derniereDate, null,
                        referentiel.personnelDuRole(role)));
            } else {
                suivi.add(new DemandeResponse.Etape(libelle, "a-venir", refusee ? "Non atteinte" : "À venir", null, null,
                        referentiel.personnelDuRole(role)));
            }
        }
        return suivi;
    }

    static List<PersonnelResponse> seul(PersonnelResponse p) {
        return p == null ? List.of() : List.of(p);
    }
}
