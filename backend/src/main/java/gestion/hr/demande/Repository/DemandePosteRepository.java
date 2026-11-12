package gestion.hr.demande.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.demande.Model.DemandePoste;

public interface DemandePosteRepository extends JpaRepository<DemandePoste, Integer> {

    List<DemandePoste> findByIdDemande(Integer idDemande);
}
