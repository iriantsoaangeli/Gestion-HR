package gestion.hr.auth.Service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;

/**
 * Utilisateur connecté (celui de la session). L'auteur d'une action vient toujours d'ici,
 * jamais du JSON envoyé par le front.
 */
@Component
public class UtilisateurCourant {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurCourant(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    public Utilisateur get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return utilisateurs.findByLogin(auth.getName()).orElseThrow();
    }
}
