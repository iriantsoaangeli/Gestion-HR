package gestion.hr.referentiel.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import gestion.hr.referentiel.Model.Poste;

public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findByIdDepartementOrderByLabel(Integer idDepartement);

    /** Département d'un compte = département de son poste. */
    @Query("select p.idDepartement from Poste p, Utilisateur u where u.id = :idUtilisateur and p.id = u.idPoste")
    Optional<Integer> departementDe(Integer idUtilisateur);

    /**
     * Postes du département que ce compte peut faire remplacer : tous, sauf ceux tenus par un autre compte
     * (les autres managers et les supérieurs). Son propre poste reste dans la liste.
     */
    @Query("""
            select p from Poste p where p.idDepartement = :idDepartement
            and not exists (select u.id from Utilisateur u where u.idPoste = p.id and u.id <> :idUtilisateur)
            order by p.label""")
    List<Poste> remplacablesPar(Integer idDepartement, Integer idUtilisateur);
}
