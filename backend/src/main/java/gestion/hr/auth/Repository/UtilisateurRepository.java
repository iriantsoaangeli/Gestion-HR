package gestion.hr.auth.Repository;

import gestion.hr.auth.Model.Utilisateur;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByLogin(String login);

    /** Vrai si ce poste est tenu par un autre compte que celui-ci (manager ou supérieur). */
    boolean existsByIdPosteAndIdNot(Integer idPoste, Integer idUtilisateur);
}
