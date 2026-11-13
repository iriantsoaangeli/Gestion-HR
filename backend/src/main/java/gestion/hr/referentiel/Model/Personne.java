package gestion.hr.referentiel.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Individu de la base (table personne) : employé (il occupe un poste, table emploi) ou externe (candidat).
 * Lu par le référentiel (détenteur d'un poste) ; créé au dépôt d'un dossier pour une nouvelle personne
 * (gestion.hr.dossier). Tous les champs sont obligatoires (NOT NULL).
 */
@Entity
@Table(name = "personne")
public class Personne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nom;
    private String prenom;
    private LocalDate naissance;
    private String adresse;
    private String tel;
    private String email;

    protected Personne() {
    }

    public Personne(String nom, String prenom, LocalDate naissance, String adresse, String tel, String email) {
        this.nom = nom;
        this.prenom = prenom;
        this.naissance = naissance;
        this.adresse = adresse;
        this.tel = tel;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public LocalDate getNaissance() {
        return naissance;
    }

    public String getAdresse() {
        return adresse;
    }

    public String getTel() {
        return tel;
    }

    public String getEmail() {
        return email;
    }

    /** « Prénom Nom » pour l'affichage. */
    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
