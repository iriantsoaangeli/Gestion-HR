package gestion.hr.dossier.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Dossier de candidature (table dossier) : une personne candidate à une offre, déposé par HR.
 * Une personne dépose au plus un dossier par offre (UNIQUE idOffre, idPersonne). Pièces jointes : pieceJointeDossier.
 */
@Entity
@Table(name = "dossier")
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime date;
    private Integer idOffre;
    private Integer idPersonne;

    protected Dossier() {
    }

    public Dossier(Integer idOffre, Integer idPersonne) {
        this.idOffre = idOffre;
        this.idPersonne = idPersonne;
        this.date = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getIdOffre() {
        return idOffre;
    }

    public Integer getIdPersonne() {
        return idPersonne;
    }
}
