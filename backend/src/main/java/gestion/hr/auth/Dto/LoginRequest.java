package gestion.hr.auth.Dto;

/** Corps de POST /api/auth/login. */
public record LoginRequest(String login, String mdp) {
}
