package gestion.hr.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.Model.DemandePoste;

public interface DemandePosteRepository extends JpaRepository<DemandePoste, Integer> {

    List<DemandePoste> findByIdDemande(Integer idDemande);
}
