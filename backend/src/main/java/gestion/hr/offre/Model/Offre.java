package gestion.hr.offre.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Offre d'emploi : table offre, issue d'une demande approuvée (idDemande). Ses postes sont dans offrePoste.
 * idPosteResponsable = poste qui suit le recrutement ; idEmetteur = compte (utilisateur) qui a publié l'offre.
 * status : OUVERT (reçoit des dossiers) ou CLOS (StatutOffre). delai et profil sont facultatifs.
 */
@Entity
@Table(name = "offre")
public class Offre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String intitule;
    private LocalDate delai;
    private Integer idPosteResponsable;
    private String description;
    private String profil;
    @Enumerated(EnumType.STRING)
    private StatutOffre status;
    private Integer idDemande;
    private Integer idEmetteur;

    protected Offre() {
    }

    public Offre(String intitule, LocalDate delai, Integer idPosteResponsable, String description, String profil,
            Integer idDemande, Integer idEmetteur) {
        this.intitule = intitule;
        this.delai = delai;
        this.idPosteResponsable = idPosteResponsable;
        this.description = description;
        this.profil = profil;
        this.idDemande = idDemande;
        this.idEmetteur = idEmetteur;
        this.status = StatutOffre.OUVERT;
    }

    public Integer getId() {
        return id;
    }

    public String getIntitule() {
        return intitule;
    }

    public LocalDate getDelai() {
        return delai;
    }

    public Integer getIdPosteResponsable() {
        return idPosteResponsable;
    }

    public String getDescription() {
        return description;
    }

    public String getProfil() {
        return profil;
    }

    public StatutOffre getStatus() {
        return status;
    }

    /** Clôture : à faire (règle « l'offre est arrêtée » de doc/conception/regle-metier/bloc2.md). Rien ne l'appelle encore. */
    public void setStatus(StatutOffre status) {
        this.status = status;
    }

    public boolean estOuverte() {
        return status == StatutOffre.OUVERT;
    }

    public Integer getIdDemande() {
        return idDemande;
    }

    public Integer getIdEmetteur() {
        return idEmetteur;
    }
}
