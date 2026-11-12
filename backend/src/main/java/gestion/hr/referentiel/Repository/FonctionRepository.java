package gestion.hr.referentiel.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.referentiel.Model.Fonction;

public interface FonctionRepository extends JpaRepository<Fonction, Integer> {

    List<Fonction> findAllByOrderByLabel();
}
