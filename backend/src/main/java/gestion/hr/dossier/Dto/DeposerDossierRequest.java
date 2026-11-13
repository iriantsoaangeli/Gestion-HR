package gestion.hr.dossier.Dto;

/**
 * Champs texte de POST /api/dossiers (multipart/form-data) ; les fichiers sont dans les parts « fichiers ».
 * Candidat : idPersonne (personne de la base) OU nom, prenom, naissance (AAAA-MM-JJ), adresse, tel, email
 * (nouvelle personne), jamais les deux. Tout est reçu en texte et vérifié par le service, pour renvoyer
 * une erreur lisible (« abc » comme idOffre, date invalide…) plutôt qu'une erreur de conversion de Spring.
 */
public record DeposerDossierRequest(
        String idOffre,
        String idPersonne,
        String nom,
        String prenom,
        String naissance,
        String adresse,
        String tel,
        String email) {
}
