package gestion.hr.dossier.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pièce jointe d'une personne (table pieceJointe) : CV, lettre… Le contenu est stocké en base (BYTEA : byte[] sans
 * annotation Lob, qui donnerait un OID sous PostgreSQL). taille = nombre d'octets. Rattachée aux dossiers par
 * pieceJointeDossier.
 * Attention : charger l'entité charge le contenu ; pour une liste, lire seulement id/nom/type/taille (requête JPQL).
 */
@Entity
@Table(name = "pieceJointe")
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPersonne;
    private String nom;
    private String type;
    private Integer taille;
    private byte[] contenu;
    private LocalDateTime date;

    protected PieceJointe() {
    }

    public PieceJointe(Integer idPersonne, String nom, String type, byte[] contenu) {
        this.idPersonne = idPersonne;
        this.nom = nom;
        this.type = type;
        this.contenu = contenu;
        this.taille = contenu.length;
        this.date = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdPersonne() {
        return idPersonne;
    }

    public String getNom() {
        return nom;
    }

    public String getType() {
        return type;
    }

    public Integer getTaille() {
        return taille;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
