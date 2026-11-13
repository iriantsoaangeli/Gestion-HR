package gestion.hr.referentiel.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Département (lecture seule ici). */
@Entity
@Table(name = "departement")
public class Departement {

    @Id
    private Integer id;
    private String code; // court et unique (RH, INF…), préfixe du code des postes
    private String label;
    private Integer nb;

    protected Departement() {
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
}
