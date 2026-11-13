package gestion.hr.entretien.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un poste (celui d'un compte) qui fait un entretien et doit l'évaluer : table interviewer, UNIQUE (idEntretien, idPoste). */
@Entity
@Table(name = "interviewer")
public class Interviewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idPoste;
    private Integer idEntretien;

    protected Interviewer() {
    }

    public Interviewer(Integer idEntretien, Integer idPoste) {
        this.idEntretien = idEntretien;
        this.idPoste = idPoste;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIdPoste() {
        return idPoste;
    }

    public Integer getIdEntretien() {
        return idEntretien;
    }
}
