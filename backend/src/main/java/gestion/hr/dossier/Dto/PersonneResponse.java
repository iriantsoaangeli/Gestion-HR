package gestion.hr.dossier.Dto;

import java.time.LocalDate;

/**
 * Personne de la base proposée comme candidat. poste et departement : poste occupé (table emploi) et son
 * département, null pour une personne externe (pas d'emploi).
 */
public record PersonneResponse(
        Integer id,
        String nom,
        String prenom,
        LocalDate naissance,
        String adresse,
        String tel,
        String email,
        String poste,
        String departement) {
}
