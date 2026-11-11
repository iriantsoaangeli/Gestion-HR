package gestion.hr.auth.Dto;

import gestion.hr.auth.Model.Role;
import gestion.hr.auth.Model.Utilisateur;

/**
 * Utilisateur renvoyé au front : jamais le mot de passe.
 * role = code (pour les règles), roleLibelle = texte à afficher.
 */
public record UtilisateurResponse(Integer id, String login, Role role, String roleLibelle) {

    public static UtilisateurResponse de(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getLogin(), u.getRole(), u.getRole().getLibelle());
    }
}
