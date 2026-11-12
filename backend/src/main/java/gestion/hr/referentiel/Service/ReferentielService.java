package gestion.hr.referentiel.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.referentiel.Dto.PersonnelResponse;
import gestion.hr.referentiel.Dto.ReferentielResponse;
import gestion.hr.referentiel.Model.Personne;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.FonctionRepository;
import gestion.hr.referentiel.Repository.PersonneRepository;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Listes de choix des formulaires.
 * - Département d'un compte = département de son poste (un compte = 1 poste d'importance).
 * - Postes à remplacer : ceux du département, sauf les postes tenus par un autre compte
 *   (autres managers, supérieurs) ; le compte connecté peut demander son propre remplacement.
 * - Personnel d'un compte : son rôle, son poste et la personne qui occupe ce poste (le détenteur).
 */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final DepartementRepository departements;
    private final FonctionRepository fonctions;
    private final PosteRepository postes;
    private final PersonneRepository personnes;
    private final UtilisateurRepository utilisateurs;
    private final UtilisateurCourant courant;

    public ReferentielService(DepartementRepository departements, FonctionRepository fonctions,
            PosteRepository postes, PersonneRepository personnes, UtilisateurRepository utilisateurs,
            UtilisateurCourant courant) {
        this.departements = departements;
        this.fonctions = fonctions;
        this.postes = postes;
        this.personnes = personnes;
        this.utilisateurs = utilisateurs;
        this.courant = courant;
    }

    public List<ReferentielResponse> departements() {
        return departements.findAllByOrderByLabel().stream()
                .map(d -> new ReferentielResponse(d.getId(), d.getLabel())).toList();
    }

    public List<ReferentielResponse> fonctions() {
        return fonctions.findAllByOrderByLabel().stream()
                .map(f -> new ReferentielResponse(f.getId(), f.getLabel())).toList();
    }

    /** Département du compte connecté (celui de ses demandes). */
    public ReferentielResponse monDepartement() {
        Utilisateur moi = courant.get();
        return postes.departementDe(moi.getId())
                .flatMap(departements::findById)
                .map(d -> new ReferentielResponse(d.getId(), d.getLabel()))
                .orElseThrow(() -> ErreurMetier.introuvable("Votre compte n'est rattaché à aucun département."));
    }

    /** Postes du département que le compte connecté peut faire remplacer. */
    public List<ReferentielResponse> postesRemplacables(Integer idDepartement) {
        return postes.remplacablesPar(idDepartement, courant.get().getId()).stream()
                .map(p -> new ReferentielResponse(p.getId(), p.getLabel())).toList();
    }

    /** Compte, rôle, poste et détenteur du poste d'un utilisateur (null si l'utilisateur est inconnu). */
    public PersonnelResponse personnel(Utilisateur u) {
        if (u == null) {
            return null;
        }
        Poste poste = postes.findById(u.getIdPoste()).orElse(null);
        String detenteur = poste == null || poste.getIdPersonne() == null ? null
                : personnes.findById(poste.getIdPersonne()).map(Personne::getNomComplet).orElse(null);
        return new PersonnelResponse(u.getId(), u.getLogin(), u.getRole().name(), u.getRole().getLibelle(),
                u.getIdPoste(), poste == null ? null : poste.getLabel(), detenteur);
    }

    /** Tout le personnel qui a ce rôle (ex. HR : les comptes qui valideront cette étape). */
    public List<PersonnelResponse> personnelDuRole(Role role) {
        return utilisateurs.findByRoleOrderByLogin(role).stream().map(this::personnel).toList();
    }
}
