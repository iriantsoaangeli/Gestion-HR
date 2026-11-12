package gestion.hr.referentiel.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.referentiel.Model.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Integer> {

    List<Departement> findAllByOrderByLabel();
}
