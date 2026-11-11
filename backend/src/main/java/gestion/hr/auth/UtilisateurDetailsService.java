package gestion.hr.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Charge un compte par son login pour Spring Security (autorité ROLE_<role>). */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurDetailsService(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        Utilisateur u = utilisateurs.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Login inconnu"));
        return User.withUsername(u.getLogin())
                .password(u.getMdp())
                .roles(u.getRole().name())
                .build();
    }
}
