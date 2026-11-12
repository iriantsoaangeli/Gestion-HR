package gestion.hr.commun.Exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** C'est un controller qui attrape les exceptions grossomodo*/
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
}
