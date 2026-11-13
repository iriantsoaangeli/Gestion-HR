package gestion.hr.dossier.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.dossier.Model.PieceJointeDossier;

/** Liens pièce jointe ↔ dossier (table pieceJointeDossier). */
public interface PieceJointeDossierRepository extends JpaRepository<PieceJointeDossier, Integer> {
}
