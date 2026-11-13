package gestion.hr.entretien.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.entretien.Repository.InterviewerRepository;

/**
 * Qui a accès aux entretiens (utilisé par les classes du package, jamais par un Controller).
 * - Lire les entretiens et évaluer : les rôles de ROLES (tous sauf FINANCE et ADMIN).
 * - Être interviewer : le poste d'un compte actif d'un de ces rôles.
 * - Préparer un entretien : HR seulement.
 */
@Component
class AccesEntretien {

    // Pourquoi je ne peux pas évaluer (raisonEvaluer de la fiche, et message d'erreur de l'évaluation)
    static final String PAS_INTERVIEWER = "Vous n'êtes pas interviewer de cet entretien.";
    static final String DEJA_EVALUE = "Vous avez déjà évalué cet entretien.";
    static final String A_VENIR = "L'entretien n'a pas encore eu lieu : vous pourrez l'évaluer après sa date.";

    /** Rôles qui lisent les entretiens et dont les postes peuvent être interviewers. */
    static final Set<Role> ROLES = EnumSet.complementOf(EnumSet.of(Role.FINANCE, Role.ADMIN));

    private final UtilisateurCourant courant;
    private final InterviewerRepository interviewers;

    AccesEntretien(UtilisateurCourant courant, InterviewerRepository interviewers) {
        this.courant = courant;
        this.interviewers = interviewers;
    }

    /** L'utilisateur connecté, s'il a accès aux entretiens (sinon 403). */
    Utilisateur lecteur() {
        Utilisateur moi = courant.get();
        if (!ROLES.contains(moi.getRole())) {
            throw ErreurMetier.accesRefuse("Les entretiens sont réservés aux rôles " + libelles() + ".");
        }
        return moi;
    }

    /** L'utilisateur connecté, s'il est HR (sinon 403). */
    Utilisateur exigerHr() {
        Utilisateur moi = courant.get();
        if (moi.getRole() != Role.HR) {
            throw ErreurMetier.accesRefuse("Seul HR prépare un entretien.");
        }
        return moi;
    }

    /** Postes qui peuvent être interviewers : ceux des comptes actifs des rôles de ROLES. */
    List<Integer> postesPossibles() {
        return interviewers.postesDesComptesActifs(ROLES.stream().map(Role::name).toList());
    }

    /** « Manager, HR, Direction » (libellés de l'enum Role). */
    static String libelles() {
        return ROLES.stream().map(Role::getLibelle).collect(Collectors.joining(", "));
    }
}
