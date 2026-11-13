package gestion.hr.offre.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.offre.Model.Offre;

/** Offres d'emploi (table offre). Utilisé aussi par les dossiers et les entretiens (lecture). */
public interface OffreRepository extends JpaRepository<Offre, Integer> {

    /** Toutes les offres, les plus récentes d'abord. */
    List<Offre> findAllByOrderByIdDesc();

    /** Offres issues d'une demande (pour savoir quelles lignes de la demande sont déjà couvertes). */
    List<Offre> findByIdDemande(Integer idDemande);
}
