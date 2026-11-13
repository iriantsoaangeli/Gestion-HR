package gestion.hr.dossier.Dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dossier de candidature renvoyé au front : le dossier (table dossier), le candidat et ses pièces jointes.
 * candidat = « Prénom Nom » ; employe = la personne occupe un poste (table emploi) ;
 * fichiers = noms des pièces jointes (affichage) ; piecesJointes = les mêmes avec id, pour les télécharger.
 */
public record DossierResponse(
        Integer id,
        LocalDateTime date,
        Integer idOffre,
        Integer idPersonne,
        String candidat,
        String email,
        String tel,
        boolean employe,
        List<String> fichiers,
        List<PieceJointeResponse> piecesJointes) {

    /** Pièce jointe sans son contenu (téléchargement : GET /api/dossiers/pieces-jointes/{id}). taille en octets. */
    public record PieceJointeResponse(Integer id, String nom, String type, Integer taille) {
    }
}
