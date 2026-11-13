package gestion.hr.offre.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.offre.Model.OffrePoste;

/** Postes couverts par une offre (table offrePoste). */
public interface OffrePosteRepository extends JpaRepository<OffrePoste, Integer> {

    List<OffrePoste> findByIdOffre(Integer idOffre);

    /** Postes d'une offre dans l'ordre où ils ont été reliés (codes créés dans l'ordre). */
    List<OffrePoste> findByIdOffreOrderByIdAsc(Integer idOffre);
}
