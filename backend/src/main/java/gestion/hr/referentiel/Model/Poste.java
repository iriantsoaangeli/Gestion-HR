package gestion.hr.referentiel.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Poste existant = un siège dans un département (lecture seule ici). */
@Entity
@Table(name = "poste")
public class Poste {

    @Id
    private Integer id;
    private String label;
    private Integer idPersonne;
    private Integer idDepartement;

    protected Poste() {
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    /** Personne qui occupe le poste (null si vacant). */
    public Integer getIdPersonne() {
        return idPersonne;
    }

    public Integer getIdDepartement() {
        return idDepartement;
    }
}
