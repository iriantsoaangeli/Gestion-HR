package gestion.hr.offre.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un poste couvert par une offre (table offrePoste) : poste existant (remplacement) ou créé avec l'offre. */
@Entity
@Table(name = "offrePoste")
public class OffrePoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idOffre;
    private Integer idPoste;

    protected OffrePoste() {
    }

    public OffrePoste(Integer idOffre, Integer idPoste) {
        this.idOffre = idOffre;
        this.idPoste = idPoste;
    }

    public Integer getIdOffre() {
        return idOffre;
    }

    public Integer getIdPoste() {
        return idPoste;
    }
}
