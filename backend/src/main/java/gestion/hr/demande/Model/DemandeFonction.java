package gestion.hr.demande.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Création de postes : une fonction et le nombre de postes voulus (le poste n'est pas encore créé). */
@Entity
@Table(name = "demandeFonction")
public class DemandeFonction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idFonction;
    private Integer idDemande;
    private Integer effectif;

    protected DemandeFonction() {
    }

    public DemandeFonction(Integer idDemande, Integer idFonction, Integer effectif) {
        this.idDemande = idDemande;
        this.idFonction = idFonction;
        this.effectif = effectif;
    }

    public Integer getIdFonction() {
        return idFonction;
    }

    public Integer getEffectif() {
        return effectif;
    }
}
