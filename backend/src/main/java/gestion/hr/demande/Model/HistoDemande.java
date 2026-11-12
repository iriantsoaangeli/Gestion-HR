package gestion.hr.demande.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Une décision sur une demande : qui (idUtilisateur), quand, et le statut obtenu. */
@Entity
@Table(name = "histoDemande")
public class HistoDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idDemande;
    private Integer idUtilisateur;

    @Column(name = "idStatus")
    @Convert(converter = StatutConverter.class)
    private Statut statut;

    private LocalDateTime date;

    protected HistoDemande() {
    }

    public HistoDemande(Integer idDemande, Integer idUtilisateur, Statut statut) {
        this.idDemande = idDemande;
        this.idUtilisateur = idUtilisateur;
        this.statut = statut;
        this.date = LocalDateTime.now();
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public Statut getStatut() {
        return statut;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
