package gestion.hr.dossier.Service;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;

/**
 * Droit d'accès aux dossiers de candidature : HR seulement (bloc 2 : « Dépôt de dossier : hr »),
 * pour le dépôt, les listes, les personnes et les pièces jointes. L'utilisateur vient de la session.
 */
@Component
class AccesDossier {

    private final UtilisateurCourant courant;

    AccesDossier(UtilisateurCourant courant) {
        this.courant = courant;
    }

    /** L'utilisateur connecté, s'il est HR (sinon 403). */
    Utilisateur exigerHr() {
        Utilisateur moi = courant.get();
        if (moi.getRole() != Role.HR) {
            throw ErreurMetier.accesRefuse("Seul HR gère les dossiers de candidature.");
        }
        return moi;
    }
}
