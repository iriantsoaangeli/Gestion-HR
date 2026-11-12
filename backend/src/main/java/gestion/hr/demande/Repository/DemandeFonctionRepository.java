package gestion.hr.demande.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.demande.Model.DemandeFonction;

public interface DemandeFonctionRepository extends JpaRepository<DemandeFonction, Integer> {

    List<DemandeFonction> findByIdDemande(Integer idDemande);
}
