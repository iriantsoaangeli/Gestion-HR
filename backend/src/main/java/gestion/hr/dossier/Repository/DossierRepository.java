package gestion.hr.dossier.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.dossier.Model.Dossier;

/** Dossiers de candidature (table dossier). Utilisé aussi par les offres et les entretiens (lecture). */
public interface DossierRepository extends JpaRepository<Dossier, Integer> {

    List<Dossier> findByIdOffre(Integer idOffre);

    Optional<Dossier> findByIdOffreAndIdPersonne(Integer idOffre, Integer idPersonne);

    List<Dossier> findAllByOrderByIdAsc();

    List<Dossier> findByIdOffreOrderByIdAsc(Integer idOffre);
}
