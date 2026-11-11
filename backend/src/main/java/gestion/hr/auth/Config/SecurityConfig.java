package gestion.hr.auth.Config;

import gestion.hr.auth.Service.UtilisateurDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Sécurité de l'API : connexion par session (cookie JSESSIONID), pas de formulaire ni de httpBasic.
 * - /api/auth/login et /api/auth/csrf sont publics, tout le reste exige d'être connecté.
 * - CSRF : cookie XSRF-TOKEN lisible par le front, renvoyé dans l'en-tête X-XSRF-TOKEN sur les POST/PUT/DELETE.
 * - Erreurs 401/403 en JSON (AuthErreurs).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthErreurs erreurs,
            SecurityContextRepository contextes) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
                        .anyRequest().authenticated())
                .securityContext(ctx -> ctx.securityContextRepository(contextes))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Jeton brut (le front renvoie la valeur du cookie telle quelle)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(erreurs)
                        .accessDeniedHandler(erreurs))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /** Où le contexte de sécurité est gardé entre deux requêtes : la session HTTP. */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Vérifie login + mot de passe contre la table utilisateur (utilisé par AuthController). */
    @Bean
    public AuthenticationManager authenticationManager(UtilisateurDetailsService comptes, PasswordEncoder encodeur) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(comptes);
        provider.setPasswordEncoder(encodeur);
        return new ProviderManager(provider);
    }
}
