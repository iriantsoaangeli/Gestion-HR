package gestion.hr.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.Model.Departement;

public interface DepartementRepository extends JpaRepository<Departement, Integer> {

    List<Departement> findAllByOrderByLabel();
}
