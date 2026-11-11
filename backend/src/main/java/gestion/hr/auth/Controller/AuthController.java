package gestion.hr.auth.Controller;

import gestion.hr.auth.Dto.LoginRequest;
import gestion.hr.auth.Dto.UtilisateurResponse;
import gestion.hr.auth.Repository.UtilisateurRepository;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Connexion à l'application.
 * POST /api/auth/login  {login, mdp} → 200 utilisateur + cookie de session, 401 IDENTIFIANTS_INVALIDES
 * GET  /api/auth/moi    → 200 utilisateur connecté, 401 sinon
 * GET  /api/auth/csrf   → pose le cookie XSRF-TOKEN (à appeler avant le premier POST)
 * POST /api/auth/logout → 204 (géré par SecurityConfig)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextes;
    private final UtilisateurRepository utilisateurs;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository contextes,
            UtilisateurRepository utilisateurs) {
        this.authenticationManager = authenticationManager;
        this.contextes = contextes;
        this.utilisateurs = utilisateurs;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest demande, HttpServletRequest request,
            HttpServletResponse response) {
        String login = demande.login() == null ? "" : demande.login().trim();
        String mdp = demande.mdp() == null ? "" : demande.mdp();

        Authentication authentification;
        try {
            authentification = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, mdp));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "code", "IDENTIFIANTS_INVALIDES",
                    "message", "Identifiant ou mot de passe incorrect."));
        }

        // Nouvel identifiant de session à la connexion (contre la fixation de session)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        // Spring Security 6 ne sauvegarde plus le contexte tout seul : on l'enregistre dans la session
        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(authentification);
        SecurityContextHolder.setContext(contexte);
        contextes.saveContext(contexte, request, response);

        return ResponseEntity.ok(UtilisateurResponse.de(utilisateurs.findByLogin(login).orElseThrow()));
    }

    @GetMapping("/moi")
    public UtilisateurResponse moi(Authentication authentification) {
        return UtilisateurResponse.de(utilisateurs.findByLogin(authentification.getName()).orElseThrow());
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken jeton) {
        // Lire le jeton suffit à faire écrire le cookie XSRF-TOKEN dans la réponse
        return jeton;
    }
}
