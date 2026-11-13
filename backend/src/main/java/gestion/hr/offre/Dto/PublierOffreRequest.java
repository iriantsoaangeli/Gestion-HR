package gestion.hr.offre.Dto;

import java.time.LocalDate;

/**
 * Corps de POST /api/offres : une demande approuvée et UNE de ses lignes,
 * soit une création (idFonction), soit un remplacement (idPoste), jamais les deux.
 * profil et delai (date limite, au moins demain) sont facultatifs.
 * L'émetteur n'est pas dans le JSON : c'est l'utilisateur connecté.
 */
public record PublierOffreRequest(
        Integer idDemande,
        Integer idFonction,
        Integer idPoste,
        String intitule,
        String description,
        String profil,
        LocalDate delai) {
}
