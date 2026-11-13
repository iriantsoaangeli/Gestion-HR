package gestion.hr.dossier.Repository;

/**
 * Ligne de PieceJointeRepository.desDossiers : une pièce jointe (sans son contenu) et le dossier auquel elle est
 * rattachée. Évite de charger le contenu des fichiers pour afficher une liste.
 */
public record PieceDuDossier(Integer idDossier, Integer id, String nom, String type, Integer taille) {
}
