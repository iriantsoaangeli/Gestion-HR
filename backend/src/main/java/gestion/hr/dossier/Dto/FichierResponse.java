package gestion.hr.dossier.Dto;

/** Pièce jointe à télécharger : nom du fichier, type MIME (déjà vérifié) et contenu. */
public record FichierResponse(String nom, String type, byte[] contenu) {
}
