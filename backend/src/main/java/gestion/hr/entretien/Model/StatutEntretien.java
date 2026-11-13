package gestion.hr.entretien.Model;

/**
 * Statut d'un entretien = ligne de la table statusEntretien (les id doivent correspondre à base/data.sql).
 * EN_ATTENTE à la préparation ; FINI quand tous les interviewers ont rendu leur évaluation (mis à jour par le backend).
 */
public enum StatutEntretien {
    EN_ATTENTE(1, "En attente"),
    FINI(2, "Fini");

    private final int id;
    private final String libelle;

    StatutEntretien(int id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public int getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public static StatutEntretien deId(int id) {
        for (StatutEntretien s : values()) {
            if (s.id == id) return s;
        }
        throw new IllegalArgumentException("Statut d'entretien inconnu : " + id);
    }
}
