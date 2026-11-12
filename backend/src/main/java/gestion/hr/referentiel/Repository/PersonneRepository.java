package gestion.hr.referentiel.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.referentiel.Model.Personne;

public interface PersonneRepository extends JpaRepository<Personne, Integer> {
}
