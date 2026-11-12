package gestion.hr.auth.Config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Erreurs de sécurité en JSON {code, message}, au lieu d'une page HTML ou d'une redirection :
 * 401 NON_CONNECTE (pas de session), 403 ACCES_REFUSE (rôle insuffisant ou jeton CSRF absent).
 */
@Component
public class AuthErreurs implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException e) throws IOException {
        ecrire(response, HttpStatus.UNAUTHORIZED, "NON_CONNECTE", "Vous devez être connecté.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException e) throws IOException {
        ecrire(response, HttpStatus.FORBIDDEN, "ACCES_REFUSE", "Accès refusé.");
    }

    static void ecrire(HttpServletResponse response, HttpStatus statut, String code, String message)
            throws IOException {
        response.setStatus(statut.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
