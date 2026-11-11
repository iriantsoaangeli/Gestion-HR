package gestion.hr.Model;

import java.util.Arrays;
import java.util.List;

import gestion.hr.auth.Model.Role;

/**
 * Statut d'une demande = ligne de la table status (les id doivent correspondre à base/data.sql).
 * Workflow : EN_ATTENTE_HR -> EN_ATTENTE_DIRECTION -> EN_ATTENTE_FINANCE -> APPROUVEE, ou REFUSEE à n'importe quelle étape.
 * valideur = rôle qui décide à cette étape (null quand la demande est terminée).
 */
public enum Statut {
    EN_ATTENTE_HR(1, "En attente HR", Role.HR),
    EN_ATTENTE_DIRECTION(2, "En attente Direction", Role.DIRECTION),
    EN_ATTENTE_FINANCE(3, "En attente Finance", Role.FINANCE),
    APPROUVEE(4, "Approuvée", null),
    REFUSEE(5, "Refusée", null);

    private final int id;
    private final String libelle;
    private final Role valideur;

    Statut(int id, String libelle, Role valideur) {
        this.id = id;
        this.libelle = libelle;
        this.valideur = valideur;
    }

    public int getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public Role getValideur() {
        return valideur;
    }

    public boolean estEnAttente() {
        return valideur != null;
    }

    /** Statut après une validation à cette étape. */
    public Statut apresValidation() {
        return switch (this) {
            case EN_ATTENTE_HR -> EN_ATTENTE_DIRECTION;
            case EN_ATTENTE_DIRECTION -> EN_ATTENTE_FINANCE;
            case EN_ATTENTE_FINANCE -> APPROUVEE;
            default -> throw new IllegalStateException("Demande déjà traitée : " + this);
        };
    }

    /** Statuts où ce rôle doit décider (ex. HR -> EN_ATTENTE_HR). Vide si le rôle ne valide pas. */
    public static List<Statut> enAttenteDe(Role role) {
        return Arrays.stream(values()).filter(s -> s.valideur != null && s.valideur == role).toList();
    }

    /** Étapes de validation, dans l'ordre du workflow. */
    public static List<Statut> etapesDeValidation() {
        return Arrays.stream(values()).filter(Statut::estEnAttente).toList();
    }

    public static Statut deId(int id) {
        for (Statut s : values()) {
            if (s.id == id) return s;
        }
        throw new IllegalArgumentException("Statut inconnu : " + id);
    }
}
