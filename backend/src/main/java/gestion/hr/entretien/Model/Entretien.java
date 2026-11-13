package gestion.hr.entretien.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entretien d'un candidat : table entretien. L'offre et le candidat se retrouvent par le dossier (idDossier).
 * Ceux qui le font sont dans interviewer, leurs évaluations dans evaluation (une par interviewer).
 */
@Entity
@Table(name = "entretien")
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime date;
    private Integer idDossier;

    @Column(name = "idStatusEntretien")
    @Convert(converter = StatutEntretienConverter.class)
    private StatutEntretien statut;

    protected Entretien() {
    }

    public Entretien(LocalDateTime date, Integer idDossier) {
        this.date = date;
        this.idDossier = idDossier;
        this.statut = StatutEntretien.EN_ATTENTE;
    }

    public Integer getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getIdDossier() {
        return idDossier;
    }

    public StatutEntretien getStatut() {
        return statut;
    }

    public void setStatut(StatutEntretien statut) {
        this.statut = statut;
    }
}
