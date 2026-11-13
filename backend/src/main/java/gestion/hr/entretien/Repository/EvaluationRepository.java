package gestion.hr.entretien.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.entretien.Model.Evaluation;

/** Évaluations des entretiens (table evaluation), une par interviewer. */
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    List<Evaluation> findByIdEntretienOrderByDateAscIdAsc(Integer idEntretien);

    long countByIdEntretien(Integer idEntretien);

    Optional<Evaluation> findByIdEntretienAndIdPoste(Integer idEntretien, Integer idPoste);
}
