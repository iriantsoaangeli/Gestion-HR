package gestion.hr.dossier.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Lien pièce jointe ↔ dossier (table pieceJointeDossier, UNIQUE idPieceJointe + idDossier).
 * Une même pièce de la personne peut servir à plusieurs de ses dossiers.
 */
@Entity
@Table(name = "pieceJointeDossier")
public class PieceJointeDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPieceJointe;
    private Integer idDossier;

    protected PieceJointeDossier() {
    }

    public PieceJointeDossier(Integer idPieceJointe, Integer idDossier) {
        this.idPieceJointe = idPieceJointe;
        this.idDossier = idDossier;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdPieceJointe() {
        return idPieceJointe;
    }

    public Integer getIdDossier() {
        return idDossier;
    }
}
