package gestion.hr.offre.Model;

/**
 * Statut d'une offre : colonne offre.status (texte, CHECK OUVERT / CLOS dans base/init.sql), lue par
 * Offre avec @Enumerated(STRING). Seule source des valeurs et du libellé affiché par le front (statutLibelle d'OffreResponse).
 * (Les prochains statuts suivent le modèle table + id de la demande et de l'entretien.)
 */
public enum StatutOffre {
    OUVERT("Ouverte"),
    CLOS("Close");

    private final String libelle;

    StatutOffre(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
