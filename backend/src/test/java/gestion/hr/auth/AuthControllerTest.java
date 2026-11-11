package gestion.hr.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@DirtiesContext
@Import({ SecurityConfig.class, UtilisateurDetailsService.class, AuthErreurs.class })
class AuthControllerTest {

    private static final String BON = "{\"login\":\"manager\",\"mdp\":\"secret\"}";
    private static final String MAUVAIS = "{\"login\":\"manager\",\"mdp\":\"faux\"}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UtilisateurRepository utilisateurs;

    @BeforeEach
    void compte() {
        Utilisateur manager = new Utilisateur(1, "manager", new BCryptPasswordEncoder().encode("secret"), Role.MANAGER);
        when(utilisateurs.findByLogin(anyString())).thenReturn(Optional.empty());
        when(utilisateurs.findByLogin("manager")).thenReturn(Optional.of(manager));
    }

    private MockHttpSession seConnecter() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BON))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void loginAvecBonMotDePasseRenvoieUtilisateurSansMdp() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("manager"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.roleLibelle").value("Manager"))
                .andExpect(jsonPath("$.mdp").doesNotExist());
    }

    @Test
    void loginAvecMauvaisMotDePasseRenvoie401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(MAUVAIS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INVALIDES"));
    }

    @Test
    void loginAvecLoginInconnuRenvoie401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"personne\",\"mdp\":\"secret\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INVALIDES"));
    }

    @Test
    void loginSansJetonCsrfRenvoie403() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void moiSansSessionRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/moi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_CONNECTE"));
    }

    @Test
    void moiApresLoginRenvoieUtilisateur() throws Exception {
        mvc.perform(get("/api/auth/moi").session(seConnecter()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("manager"));
    }

    @Test
    void logoutFermeLaSession() throws Exception {
        MockHttpSession session = seConnecter();
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/moi").session(session))
                .andExpect(status().isUnauthorized());
    }
}
