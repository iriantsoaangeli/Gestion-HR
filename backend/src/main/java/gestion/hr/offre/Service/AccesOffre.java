package gestion.hr.offre.Service;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Service.UtilisateurCourant;
import gestion.hr.commun.Exception.ErreurMetier;

/** Droit d'accès aux offres d'emploi : HR seulement (publier, lister, fiche, demandes approuvées). */
@Component
class AccesOffre {

    private final UtilisateurCourant courant;

    AccesOffre(UtilisateurCourant courant) {
        this.courant = courant;
    }

    /** L'utilisateur connecté, s'il est HR (sinon 403). */
    Utilisateur exigerHr() {
        Utilisateur moi = courant.get();
        if (moi.getRole() != Role.HR) {
            throw ErreurMetier.accesRefuse("Seul HR gère les offres d'emploi.");
        }
        return moi;
    }
}
