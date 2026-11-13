package gestion.hr.offre.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Emploi (table emploi) : qui occupe un poste. idPersonne null = poste vacant. Un poste a une seule ligne (UNIQUE).
 * Écrit ici seulement à la création d'un poste vide (publication d'une offre).
 */
@Entity
@Table(name = "emploi")
public class Emploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPoste;
    private Integer idPersonne;

    protected Emploi() {
    }

    /** Emploi vacant d'un poste qui vient d'être créé. */
    public static Emploi vacant(Integer idPoste) {
        Emploi e = new Emploi();
        e.idPoste = idPoste;
        return e;
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public Integer getIdPersonne() {
        return idPersonne;
    }
}
