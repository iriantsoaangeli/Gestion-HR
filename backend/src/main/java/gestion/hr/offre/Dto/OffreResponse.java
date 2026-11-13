package gestion.hr.offre.Dto;

import java.time.LocalDate;
import java.util.List;

import gestion.hr.offre.Model.StatutOffre;

/**
 * Offre renvoyée au front, avec ses libellés (doc/routes/api/offres.md).
 * departement : DepartementOffre (premier poste couvert, sinon poste responsable) ; responsable : libellé du poste responsable ;
 * statut / statutLibelle : colonne offre.status (StatutOffre) ; ouverte : reçoit des dossiers et des entretiens.
 * postes : postes couverts (offrePoste) avec leur code ; candidats : nombre de dossiers déposés.
 */
public record OffreResponse(
        Integer id,
        String intitule,
        Integer idDepartement,
        String departement,
        Integer idPosteResponsable,
        String responsable,
        String description,
        String profil,
        StatutOffre statut,
        String statutLibelle,
        boolean ouverte,
        LocalDate delai,
        Integer idDemande,
        Integer idEmetteur,
        String emetteur,
        List<PosteCouvert> postes,
        int candidats) {

    public record PosteCouvert(Integer id, String code, String label) {
    }
}
