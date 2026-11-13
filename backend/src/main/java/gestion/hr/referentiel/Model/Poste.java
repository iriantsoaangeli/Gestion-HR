package gestion.hr.referentiel.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Poste = un siège dans un département. Son détenteur est dans la table emploi.
 * Lu par le référentiel et les demandes ; créé seulement à la publication d'une offre (gestion.hr.offre).
 */
@Entity
@Table(name = "poste")
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String code; // identifiant lisible et unique : <code du département>-<année>-<compteur>
    private String label;
    private Integer idDepartement;

    protected Poste() {
    }

    /** Nouveau poste (vide) : code lisible déjà calculé, label = la fonction. */
    public Poste(String code, String label, Integer idDepartement) {
        this.code = code;
        this.label = label;
        this.idDepartement = idDepartement;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Integer getIdDepartement() {
        return idDepartement;
    }
}
