package gestion.hr.Dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Corps de POST /api/demandes.
 * creations : postes à créer [{idFonction, effectif}] — remplacements : id des postes existants à remplir.
 * delai : date limite facultative (au moins une semaine après aujourd'hui).
 * Le demandeur n'est pas dans le JSON : c'est l'utilisateur connecté.
 */
public record CreerDemandeRequest(
        Integer idDepartement,
        String motif,
        LocalDate delai,
        List<Creation> creations,
        List<Integer> remplacements) {

    public record Creation(Integer idFonction, Integer effectif) {
    }
}
