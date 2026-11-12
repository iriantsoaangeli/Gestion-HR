package gestion.hr.referentiel.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import gestion.hr.referentiel.Model.Personne;

public interface PersonneRepository extends JpaRepository<Personne, Integer> {

    /** Personne qui occupe le poste (table emploi), vide si le poste est vacant. */
    @Query(value = """
            SELECT p.* FROM personne p
            JOIN emploi e ON e.idPersonne = p.id
            WHERE e.idPoste = :idPoste""", nativeQuery = true)
    Optional<Personne> detenteurDuPoste(Integer idPoste);
}
