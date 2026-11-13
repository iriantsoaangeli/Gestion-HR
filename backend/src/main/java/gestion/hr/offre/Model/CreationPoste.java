package gestion.hr.offre.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Date de création d'un poste (table creationPoste), écrite quand une offre crée un poste vide. */
@Entity
@Table(name = "creationPoste")
public class CreationPoste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPoste;
    private LocalDateTime date;

    protected CreationPoste() {
    }

    public CreationPoste(Integer idPoste) {
        this.idPoste = idPoste;
        this.date = LocalDateTime.now();
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
