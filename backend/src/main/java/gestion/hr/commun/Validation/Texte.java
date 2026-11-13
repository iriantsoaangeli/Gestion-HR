package gestion.hr.commun.Validation;

import gestion.hr.commun.Exception.ErreurMetier;

/**
 * Contrôle des champs texte reçus du front : vide, obligatoire, facultatif, longueur maximale.
 * nom = le champ tel qu'il apparaît dans le message (« Le nom », « L'e-mail », « Les remarques »…) ;
 * un nom qui commence par « Les » met le message au pluriel.
 */
public final class Texte {

    private Texte() {
    }

    /** Vrai si le texte est null, vide ou seulement des espaces. */
    public static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    /** Le texte sans espaces autour ; 400 VALIDATION s'il est vide ou trop long. */
    public static String obligatoire(String valeur, String nom, int max) {
        if (vide(valeur)) {
            throw ErreurMetier.validation(nom + (pluriel(nom) ? " sont obligatoires." : " est obligatoire."));
        }
        return borne(valeur.trim(), nom, max);
    }

    /** Le texte sans espaces autour, ou null s'il est vide ; 400 VALIDATION s'il est trop long. */
    public static String facultatif(String valeur, String nom, int max) {
        return vide(valeur) ? null : borne(valeur.trim(), nom, max);
    }

    private static String borne(String texte, String nom, int max) {
        if (texte.length() > max) {
            throw ErreurMetier.validation(nom + (pluriel(nom) ? " ne doivent pas" : " ne doit pas") + " dépasser "
                    + max + " caractères.");
        }
        return texte;
    }

    private static boolean pluriel(String nom) {
        return nom.startsWith("Les ");
    }
}
