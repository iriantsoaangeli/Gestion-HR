package gestion.hr.offre.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fonction donnée à un poste (table fonctionPoste) : écrite à la création d'un poste vide par une offre. */
@Entity
@Table(name = "fonctionPoste")
public class FonctionPoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPoste;
    private Integer idFonction;

    protected FonctionPoste() {
    }

    public FonctionPoste(Integer idPoste, Integer idFonction) {
        this.idPoste = idPoste;
        this.idFonction = idFonction;
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public Integer getIdFonction() {
        return idFonction;
    }
}
