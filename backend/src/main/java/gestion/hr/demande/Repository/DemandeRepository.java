package gestion.hr.demande.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gestion.hr.demande.Model.Demande;
import gestion.hr.demande.Model.Statut;

public interface DemandeRepository extends JpaRepository<Demande, Integer> {

    /** Demandes créées par un utilisateur, les plus récentes d'abord. */
    List<Demande> findByIdDemandeurOrderByDateDescIdDesc(Integer idDemandeur);

    List<Demande> findAllByOrderByDateDescIdDesc();

    /** Demandes en attente à ces statuts, les plus anciennes d'abord (file à traiter). */
    List<Demande> findByStatutInOrderByDateAscIdAsc(List<Statut> statuts);

    /** Demandes sur lesquelles un utilisateur a pris une décision (validation ou refus). */
    @Query("""
            select d from Demande d
            where d.id in (select h.idDemande from HistoDemande h where h.idUtilisateur = :idUtilisateur)
            order by d.date desc, d.id desc""")
    List<Demande> findTraiteesPar(@Param("idUtilisateur") Integer idUtilisateur);
}
