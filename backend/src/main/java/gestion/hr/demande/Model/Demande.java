package gestion.hr.demande.Model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Demande de personnel : table demande. Les postes demandés sont dans demandeFonction (créations)
 * et demandePoste (remplacements) ; les décisions dans histoDemande.
 */
@Entity
@Table(name = "demande")
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idDepartement;

    @Column(name = "idStatus")
    @Convert(converter = StatutConverter.class)
    private Statut statut;

    private Integer idDemandeur;
    private LocalDate date;
    private String motif;
    private LocalDate delai;

    protected Demande() {
    }

    public Demande(Integer idDepartement, Integer idDemandeur, String motif, LocalDate delai) {
        this.idDepartement = idDepartement;
        this.idDemandeur = idDemandeur;
        this.motif = motif;
        this.delai = delai;
        this.statut = Statut.EN_ATTENTE_HR;
        this.date = LocalDate.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdDepartement() {
        return idDepartement;
    }

    public Statut getStatut() {
        return statut;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public Integer getIdDemandeur() {
        return idDemandeur;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMotif() {
        return motif;
    }

    public LocalDate getDelai() {
        return delai;
    }
}
