package gestion.hr.entretien.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gestion.hr.entretien.Model.Interviewer;

/** Interviewers d'un entretien (table interviewer), et les postes qui peuvent en être. */
public interface InterviewerRepository extends JpaRepository<Interviewer, Integer> {

    List<Interviewer> findByIdEntretienOrderByIdAsc(Integer idEntretien);

    long countByIdEntretien(Integer idEntretien);

    boolean existsByIdEntretienAndIdPoste(Integer idEntretien, Integer idPoste);

    /**
     * Postes des comptes actifs (utilisateur.status) qui ont un de ces rôles. Requête SQL : la colonne status
     * n'est pas dans l'entité Utilisateur.
     */
    @Query(value = "SELECT u.idPoste FROM utilisateur u WHERE u.status AND u.role IN (:roles) ORDER BY u.idPoste",
            nativeQuery = true)
    List<Integer> postesDesComptesActifs(@Param("roles") Collection<String> roles);

    /** Login du compte qui tient ce poste (vide si aucun compte). */
    @Query("select u.login from Utilisateur u where u.idPoste = :idPoste")
    Optional<String> loginDuPoste(@Param("idPoste") Integer idPoste);
}
