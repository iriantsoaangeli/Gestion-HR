package gestion.hr.Exception;

import org.springframework.http.HttpStatus;

/**
 * Règle de gestion non respectée. Renvoyée au front en JSON {code, message} par GestionErreurs.
 * Ex. : 400 VALIDATION, 403 ACCES_REFUSE, 404 RESSOURCE_INTROUVABLE, 409 TRANSITION_INTERDITE.
 */
public class ErreurMetier extends RuntimeException {

    private final HttpStatus statut;
    private final String code;

    public ErreurMetier(HttpStatus statut, String code, String message) {
        super(message);
        this.statut = statut;
        this.code = code;
    }

    public static ErreurMetier validation(String message) {
        return new ErreurMetier(HttpStatus.BAD_REQUEST, "VALIDATION", message);
    }

    public static ErreurMetier introuvable(String message) {
        return new ErreurMetier(HttpStatus.NOT_FOUND, "RESSOURCE_INTROUVABLE", message);
    }

    public static ErreurMetier accesRefuse(String message) {
        return new ErreurMetier(HttpStatus.FORBIDDEN, "ACCES_REFUSE", message);
    }

    public HttpStatus getStatut() {
        return statut;
    }

    public String getCode() {
        return code;
    }
}
