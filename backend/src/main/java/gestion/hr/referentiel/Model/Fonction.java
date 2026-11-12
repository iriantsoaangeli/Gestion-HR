package gestion.hr.referentiel.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fonction (le travail d'un poste) : une création de poste désigne une fonction. Lecture seule ici. */
@Entity
@Table(name = "fonction")
public class Fonction {

    @Id
    private Integer id;
    private String label;

    protected Fonction() {
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
