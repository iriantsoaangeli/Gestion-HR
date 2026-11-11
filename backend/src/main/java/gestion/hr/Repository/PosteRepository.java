package gestion.hr.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.Model.Poste;

public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findByIdDepartementOrderByLabel(Integer idDepartement);
}
