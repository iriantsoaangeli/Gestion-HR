package gestion.hr.commun.Exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Grossomodo , C"est un listner d'exception sur les controllers*/
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(ErreurMetier.class)
    public ResponseEntity<Map<String, String>> metier(ErreurMetier e) {
        return ResponseEntity.status(e.getStatut()).body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> illisible(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "REQUETE_ILLISIBLE", "message", "Le corps de la requête est invalide."));
    }

    /** Envoi multipart au-delà de spring.servlet.multipart.max-request-size (dépôt de dossier). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tropVolumineux(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION", "message", "Les pièces jointes dépassent 30 Mo au total."));
    }
}
