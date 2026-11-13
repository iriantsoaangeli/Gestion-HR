package gestion.hr.dossier.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import gestion.hr.dossier.Model.PieceJointe;

/** Pièces jointes des personnes (table pieceJointe). */
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Integer> {

    /** Pièces jointes de ces dossiers, sans leur contenu, dans l'ordre d'ajout. */
    @Query("""
            select new gestion.hr.dossier.Repository.PieceDuDossier(l.idDossier, p.id, p.nom, p.type, p.taille)
            from PieceJointe p, PieceJointeDossier l
            where l.idPieceJointe = p.id and l.idDossier in :idsDossiers
            order by p.id""")
    List<PieceDuDossier> desDossiers(Collection<Integer> idsDossiers);
}
