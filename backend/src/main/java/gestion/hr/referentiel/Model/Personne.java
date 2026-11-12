package gestion.hr.referentiel.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Individu de la base (table personne) : le détenteur d'un poste (poste.idPersonne). Lecture seule ici. */
@Entity
@Table(name = "personne")
public class Personne {

    @Id
    private Integer id;
    private String nom;
    private String prenom;

    protected Personne() {
    }

    public Integer getId() {
        return id;
    }

    /** « Prénom Nom » pour l'affichage. */
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
