package gestion.hr.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Classe à part : le post-processeur csrf() de spring-security-test remplace le dépôt de jetons
 * dans le contexte partagé, ce qui empêcherait de voir le vrai cookie XSRF-TOKEN.
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, UtilisateurDetailsService.class, AuthErreurs.class })
@DirtiesContext
class CsrfCookieTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UtilisateurRepository utilisateurs;

    @Test
    void csrfPoseLeCookieLisibleParLeFront() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}
