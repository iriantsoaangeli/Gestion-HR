package gestion.hr.auth;

/** Corps de POST /api/auth/login. */
public record LoginRequest(String login, String mdp) {
}
