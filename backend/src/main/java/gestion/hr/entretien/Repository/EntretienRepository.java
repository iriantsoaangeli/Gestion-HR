package gestion.hr.entretien.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gestion.hr.entretien.Model.Entretien;
import gestion.hr.entretien.Model.StatutEntretien;

/** Entretiens (table entretien). */
public interface EntretienRepository extends JpaRepository<Entretien, Integer> {

    List<Entretien> findByStatutOrderByDateAscIdAsc(StatutEntretien statut);

    List<Entretien> findByStatutOrderByDateDescIdDesc(StatutEntretien statut);

    List<Entretien> findAllByOrderByDateAscIdAsc();

    List<Entretien> findByIdDossierInOrderByDateAscIdAsc(List<Integer> idDossiers);

    /** Entretiens dont ce poste est interviewer et qu'il n'a pas encore évalués (les plus proches d'abord). */
    @Query("""
            select e from Entretien e
            where e.id in (select i.idEntretien from Interviewer i where i.idPoste = :idPoste)
            and e.id not in (select v.idEntretien from Evaluation v where v.idPoste = :idPoste)
            order by e.date asc, e.id asc""")
    List<Entretien> aEvaluerPar(@Param("idPoste") Integer idPoste);

    /** Entretiens que ce poste a déjà évalués (les plus récents d'abord). */
    @Query("""
            select e from Entretien e
            where e.id in (select v.idEntretien from Evaluation v where v.idPoste = :idPoste)
            order by e.date desc, e.id desc""")
    List<Entretien> evaluesPar(@Param("idPoste") Integer idPoste);
}
