package gestion.hr.demande.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Remplacement : un poste existant à remplir (1 poste = 1 siège, pas d'effectif). */
@Entity
@Table(name = "demandePoste")
public class DemandePoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPoste;
    private Integer idDemande;

    protected DemandePoste() {
    }

    public DemandePoste(Integer idDemande, Integer idPoste) {
        this.idDemande = idDemande;
        this.idPoste = idPoste;
    }

    public Integer getIdPoste() {
        return idPoste;
    }
}
