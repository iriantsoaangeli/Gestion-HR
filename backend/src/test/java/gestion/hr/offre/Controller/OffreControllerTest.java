package gestion.hr.offre.Controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Offres d'emploi sur la base locale (base/init.sql + data.sql + demo.sql : 3 offres de démo, dont une close).
 * Chaque test est annulé à la fin (@Transactional) : la base n'est pas modifiée.
 * Les demandes de démo sont déjà toutes couvertes : chaque test qui publie crée et fait approuver sa demande
 * (Informatique : 2 Développeurs (fonction 3) + remplacement du poste 4). Poste 6 = Manager informatique (manager).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static MockHttpServletRequestBuilder en(String login, MockHttpServletRequestBuilder req) {
        return req.with(user(login)).with(csrf());
    }

    private ResultActions publier(String login, String json) throws Exception {
        return mvc.perform(en(login, post("/api/offres")).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Corps de publication : ligne = "\"idFonction\": 3" ou "\"idPoste\": 4". */
    private static String offre(int idDemande, String ligne) {
        return """
                {"idDemande": %d, %s, "intitule": "Développeur web", "description": "Vue.js et Spring Boot"}"""
                .formatted(idDemande, ligne);
    }

    /** Demande du manager (Informatique), approuvée si demandé. */
    private int demande(boolean approuvee) throws Exception {
        String json = mvc.perform(en("manager", post("/api/demandes")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idDepartement": 2, "motif": "Test offres",
                         "creations": [{"idFonction": 3, "effectif": 2}], "remplacements": [4]}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.id");
        if (approuvee) {
            for (String login : List.of("hr", "direction", "finance")) {
                mvc.perform(en(login, post("/api/demandes/" + id + "/valider"))).andExpect(status().isOk());
            }
        }
        return id;
    }

    // ---------- Lecture ----------

    @Test
    void hrListeLesOffresDeDemo() throws Exception {
        mvc.perform(get("/api/offres").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].intitule", hasItem("Comptable")))
                .andExpect(jsonPath("$[*].statutLibelle", hasItem("Close")))
                .andExpect(jsonPath("$[?(@.intitule == 'Comptable')].postes[*].code", hasItem("FIN-2026-0004")))
                .andExpect(jsonPath("$[?(@.intitule == 'Développeur')].responsable", hasItem("Manager informatique")));
    }

    @Test
    void offresReserveesAHr() throws Exception {
        mvc.perform(get("/api/offres").with(user("manager")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
        mvc.perform(get("/api/offres/demandes-approuvees").with(user("direction"))).andExpect(status().isForbidden());
        publier("finance", offre(1, "\"idFonction\": 3")).andExpect(status().isForbidden());
    }

    @Test
    void offreInconnue() throws Exception {
        mvc.perform(get("/api/offres/999999").with(user("hr")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"));
    }

    @Test
    void demandesApprouveesIndiquentLesLignesCouvertes() throws Exception {
        int id = demande(true);
        mvc.perform(get("/api/offres/demandes-approuvees").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].demandeur.login", hasItem("manager")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].demandeur.detenteur", hasItem("Hery Rakotomalala")))
                .andExpect(jsonPath("$[?(@.motif == 'Clôture annuelle : renfort comptable')].creations[0].idOffre")
                        .isNotEmpty());
    }

    // ---------- Publier ----------

    @Test
    void publierUneCreationCreeLesPostesVides() throws Exception {
        int id = demande(true);
        String prefixe = "INF-" + LocalDate.now().getYear() + "-";
        Integer max = jdbc.queryForObject(
                "SELECT max(CAST(substring(code FROM ?) AS INTEGER)) FROM poste WHERE code LIKE ?",
                Integer.class, prefixe.length() + 1, prefixe + "%");
        int suivant = (max == null ? 0 : max) + 1;

        String json = publier("hr", offre(id, "\"idFonction\": 3"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.intitule").value("Développeur web"))
                .andExpect(jsonPath("$.departement").value("Informatique"))
                .andExpect(jsonPath("$.idPosteResponsable").value(6))
                .andExpect(jsonPath("$.responsable").value("Manager informatique"))
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andExpect(jsonPath("$.ouverte").value(true))
                .andExpect(jsonPath("$.statutLibelle").value("Ouverte"))
                .andExpect(jsonPath("$.emetteur").value("hr"))
                .andExpect(jsonPath("$.idDemande").value(id))
                .andExpect(jsonPath("$.candidats").value(0))
                .andExpect(jsonPath("$.postes.length()").value(2))
                .andExpect(jsonPath("$.postes[*].label", everyItem(startsWith("Développeur"))))
                .andExpect(jsonPath("$.postes[0].code").value(prefixe + String.format("%04d", suivant)))
                .andExpect(jsonPath("$.postes[1].code").value(prefixe + String.format("%04d", suivant + 1)))
                .andReturn().getResponse().getContentAsString();

        // Chaque poste créé : fonction, emploi vacant, date de création
        List<Integer> postes = JsonPath.read(json, "$.postes[*].id");
        for (Integer p : postes) {
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM fonctionPoste WHERE idPoste = ? AND idFonction = 3",
                    Integer.class, p));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM emploi WHERE idPoste = ? AND idPersonne IS NULL",
                    Integer.class, p));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM creationPoste WHERE idPoste = ?",
                    Integer.class, p));
        }
        int idOffre = JsonPath.read(json, "$.id");
        mvc.perform(get("/api/offres/" + idOffre).with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postes.length()").value(2));
    }

    @Test
    void publierUnRemplacementCouvreLePosteExistant() throws Exception {
        int id = demande(true);
        publier("hr", """
                {"idDemande": %d, "idPoste": 4, "intitule": "Administrateur système", "description": "Linux",
                 "profil": "  ", "delai": "%s"}""".formatted(id, LocalDate.now().plusDays(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postes.length()").value(1))
                .andExpect(jsonPath("$.postes[0].id").value(4))
                .andExpect(jsonPath("$.postes[0].code").value("INF-2026-0002"))
                .andExpect(jsonPath("$.profil").doesNotExist())
                .andExpect(jsonPath("$.delai").value(LocalDate.now().plusDays(1).toString()));
    }

    @Test
    void ligneDejaPublieeRefusee() throws Exception {
        int id = demande(true);
        publier("hr", offre(id, "\"idPoste\": 4")).andExpect(status().isCreated());
        publier("hr", offre(id, "\"idPoste\": 4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_FAIT"));
        publier("hr", offre(id, "\"idFonction\": 3")).andExpect(status().isCreated());
        publier("hr", offre(id, "\"idFonction\": 3")).andExpect(status().isConflict());

        mvc.perform(get("/api/offres/demandes-approuvees").with(user("hr")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].creations[0].idOffre").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].remplacements[0].idOffre").isNotEmpty());
    }

    @Test
    void demandeNonApprouveeRefusee() throws Exception {
        int id = demande(false);
        publier("hr", offre(id, "\"idFonction\": 3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void demandeOuLigneInconnueRefusee() throws Exception {
        int id = demande(true);
        publier("hr", offre(999999, "\"idFonction\": 3")).andExpect(status().isBadRequest());
        publier("hr", offre(id, "\"idFonction\": 1")).andExpect(status().isBadRequest());
        publier("hr", offre(id, "\"idPoste\": 3")).andExpect(status().isBadRequest());
        publier("hr", offre(id, "\"idFonction\": 3, \"idPoste\": 4")).andExpect(status().isBadRequest());
        publier("hr", offre(id, "\"idFonction\": null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Choisissez un seul poste de la demande : une création ou un remplacement."));
    }

    @Test
    void champsObligatoiresEtDateLimite() throws Exception {
        int id = demande(true);
        publier("hr", """
                {"idDemande": %d, "idFonction": 3, "intitule": "   ", "description": "x"}""".formatted(id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("L'intitulé de l'offre est obligatoire."));
        publier("hr", """
                {"idDemande": %d, "idFonction": 3, "intitule": "Dev"}""".formatted(id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La description de l'offre est obligatoire."));
        for (LocalDate date : List.of(LocalDate.now(), LocalDate.now().minusDays(1))) {
            publier("hr", """
                    {"idDemande": %d, "idFonction": 3, "intitule": "Dev", "description": "x", "delai": "%s"}"""
                    .formatted(id, date))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("La date limite de candidature doit être au moins demain."));
        }
    }

    @Test
    void publierSansCsrfRefuse() throws Exception {
        int id = demande(true);
        mvc.perform(post("/api/offres").with(user("hr")).contentType(MediaType.APPLICATION_JSON)
                .content(offre(id, "\"idFonction\": 3")))
                .andExpect(status().isForbidden());
    }
}
