package gestion.hr.entretien.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Évaluation d'un entretien par un interviewer : table evaluation, UNIQUE (idEntretien, idPoste).
 * idPoste = l'interviewer qui l'a écrite ; note entière de 0 à 100 ; remarques obligatoires.
 */
@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idEntretien;
    private Integer idPoste;
    private Integer note;
    private String remarques;
    private LocalDateTime date;

    protected Evaluation() {
    }

    public Evaluation(Integer idEntretien, Integer idPoste, Integer note, String remarques) {
        this.idEntretien = idEntretien;
        this.idPoste = idPoste;
        this.note = note;
        this.remarques = remarques;
        this.date = LocalDateTime.now().withNano(0);
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdEntretien() {
        return idEntretien;
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public Integer getNote() {
        return note;
    }

    public String getRemarques() {
        return remarques;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
