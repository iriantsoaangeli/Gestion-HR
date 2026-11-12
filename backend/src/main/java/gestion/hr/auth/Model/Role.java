package gestion.hr.auth.Model;

/**
 * Rôle d'un utilisateur, stocké en texte dans {@code utilisateur.role}.
 * Workflow d'une demande : MANAGER crée, puis HR, DIRECTION et FINANCE valident.
 * Côté Spring Security, chaque rôle devient l'autorité {@code ROLE_<nom>}.
 * Le libellé est la seule source du texte affiché par le front (pas de copie côté vue).
 */
public enum Role {
    MANAGER("Manager"),
    HR("HR"),
    DIRECTION("Direction"),
    FINANCE("Finance"),
    ADMIN("Administrateur");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Seul un manager crée des demandes de personnel. */
    public boolean peutCreerDemande() {
        return this == MANAGER;
    }

    /** Seuls ADMIN et DIRECTION voient toutes les demandes ; les autres ne voient que les leurs. */
    public boolean voitToutesLesDemandes() {
        return this == ADMIN || this == DIRECTION;
    }
}
