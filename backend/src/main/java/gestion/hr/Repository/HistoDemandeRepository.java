package gestion.hr.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.Model.HistoDemande;

public interface HistoDemandeRepository extends JpaRepository<HistoDemande, Integer> {

    List<HistoDemande> findByIdDemandeOrderByDateAscIdAsc(Integer idDemande);

    boolean existsByIdDemandeAndIdUtilisateur(Integer idDemande, Integer idUtilisateur);
}
