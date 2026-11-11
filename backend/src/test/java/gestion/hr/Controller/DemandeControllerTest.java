package gestion.hr.Controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Workflow complet des demandes sur la base locale (base/init.sql + base/data.sql).
 * Chaque test est annulé à la fin (@Transactional) : la base n'est pas modifiée.
 * Comptes utilisés : manager, hr, direction, finance (data.sql) ; départements 1-3, fonctions 1-6, postes 1-5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandeControllerTest {

    @Autowired
    private MockMvc mvc;

    private static MockHttpServletRequestBuilder en(String login, MockHttpServletRequestBuilder req) {
        return req.with(user(login)).with(csrf());
    }

    private ResultActions creer(String login, String json) throws Exception {
        return mvc.perform(en(login, post("/api/demandes")).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Département Informatique (2) : création de 2 Développeurs (fonction 3) + remplacement du poste 4. */
    private static final String DEMANDE = """
            {"idDepartement": 2, "motif": "Nouveau projet web",
             "creations": [{"idFonction": 3, "effectif": 2}], "remplacements": [4]}""";

    private int creerDemande() throws Exception {
        String json = creer("manager", DEMANDE).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions decider(String login, int id, String action) throws Exception {
        return mvc.perform(en(login, post("/api/demandes/" + id + "/" + action)));
    }

    @Test
    void managerCreeUneDemandeEnAttenteHr() throws Exception {
        creer("manager", DEMANDE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_HR"))
                .andExpect(jsonPath("$.etapeValideur").value("HR"))
                .andExpect(jsonPath("$.demandeur").value("manager"))
                .andExpect(jsonPath("$.departement").value("Informatique"))
                .andExpect(jsonPath("$.creations[0].fonction").value("Développeur"))
                .andExpect(jsonPath("$.creations[0].effectif").value(2))
                .andExpect(jsonPath("$.remplacements[0].idPoste").value(4))
                .andExpect(jsonPath("$.historique").isEmpty());
    }

    @Test
    void seulUnManagerPeutCreer() throws Exception {
        creer("hr", DEMANDE).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void demandeSansPosteRefusee() throws Exception {
        creer("manager", """
                {"idDepartement": 2, "motif": "x", "creations": [], "remplacements": []}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ajoutez ou remplacez au moins un poste."));
    }

    @Test
    void posteDUnAutreDepartementRefuse() throws Exception {
        creer("manager", """
                {"idDepartement": 2, "motif": "x", "remplacements": [5]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void delaiTropCourtRefuse() throws Exception {
        creer("manager", """
                {"idDepartement": 2, "motif": "x", "remplacements": [4], "delai": "%s"}"""
                .formatted(LocalDate.now().plusDays(3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le délai doit être d'au moins une semaine."));
    }

    @Test
    void effectifNulRefuse() throws Exception {
        creer("manager", """
                {"idDepartement": 2, "motif": "x", "creations": [{"idFonction": 3, "effectif": 0}]}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void workflowCompletJusquaApprouvee() throws Exception {
        int id = creerDemande();
        decider("hr", id, "valider").andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_DIRECTION"));
        decider("direction", id, "valider").andExpect(jsonPath("$.statut").value("EN_ATTENTE_FINANCE"));
        decider("finance", id, "valider")
                .andExpect(jsonPath("$.statut").value("APPROUVEE"))
                .andExpect(jsonPath("$.etapeValideur").doesNotExist())
                .andExpect(jsonPath("$.historique.length()").value(3))
                .andExpect(jsonPath("$.historique[0].utilisateur").value("hr"))
                .andExpect(jsonPath("$.historique[2].statut").value("APPROUVEE"));
    }

    @Test
    void mauvaiseEtapeRefusee() throws Exception {
        int id = creerDemande();
        decider("direction", id, "valider")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
        decider("manager", id, "valider").andExpect(status().isForbidden());
    }

    @Test
    void refusArreteLeWorkflow() throws Exception {
        int id = creerDemande();
        decider("hr", id, "refuser").andExpect(jsonPath("$.statut").value("REFUSEE"));
        decider("direction", id, "valider")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSITION_INTERDITE"));
    }

    @Test
    void demandeInconnue() throws Exception {
        decider("hr", 999999, "valider").andExpect(status().isNotFound());
    }

    @Test
    void mesDemandesEtDemandesTraitees() throws Exception {
        int id = creerDemande();
        decider("hr", id, "valider");

        mvc.perform(get("/api/demandes/mes-demandes").with(user("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get("/api/demandes/mes-demandes").with(user("hr")))
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));

        mvc.perform(get("/api/demandes/traitees").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get("/api/demandes/traitees").with(user("direction")))
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));
    }

    @Test
    void ficheVisibleParLesConcernesSeulement() throws Exception {
        int id = creerDemande();
        mvc.perform(get("/api/demandes/" + id).with(user("manager"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.aMoiDeDecider").value(false));
        mvc.perform(get("/api/demandes/" + id).with(user("hr"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.aMoiDeDecider").value(true));
        mvc.perform(get("/api/demandes/" + id).with(user("direction"))).andExpect(status().isOk());
        // Finance n'est pas encore concernée
        mvc.perform(get("/api/demandes/" + id).with(user("finance"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/demandes/999999").with(user("hr"))).andExpect(status().isNotFound());
    }

    @Test
    void toutesReserveeAdminEtDirection() throws Exception {
        int id = creerDemande();
        mvc.perform(get("/api/demandes").with(user("direction"))).andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get("/api/demandes").with(user("admin"))).andExpect(status().isOk());
        mvc.perform(get("/api/demandes").with(user("manager"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/demandes").with(user("hr"))).andExpect(status().isForbidden());
    }

    @Test
    void aTraiterSuitLEtape() throws Exception {
        int id = creerDemande();
        mvc.perform(get("/api/demandes/a-traiter").with(user("hr"))).andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get("/api/demandes/a-traiter").with(user("direction")))
                .andExpect(jsonPath("$[*].id", not(hasItem(id))));
        decider("hr", id, "valider");
        mvc.perform(get("/api/demandes/a-traiter").with(user("hr"))).andExpect(jsonPath("$[*].id", not(hasItem(id))));
        mvc.perform(get("/api/demandes/a-traiter").with(user("direction")))
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(get("/api/demandes/a-traiter").with(user("manager"))).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void suiviCalculeParLeServeur() throws Exception {
        int id = creerDemande();
        decider("hr", id, "valider");
        decider("direction", id, "refuser")
                .andExpect(jsonPath("$.suivi.length()").value(4))
                .andExpect(jsonPath("$.suivi[0].etat").value("faite"))
                .andExpect(jsonPath("$.suivi[0].utilisateur").value("manager"))
                .andExpect(jsonPath("$.suivi[1].libelle").value("HR"))
                .andExpect(jsonPath("$.suivi[1].etat").value("faite"))
                .andExpect(jsonPath("$.suivi[2].etat").value("refusee"))
                .andExpect(jsonPath("$.suivi[2].utilisateur").value("direction"))
                .andExpect(jsonPath("$.suivi[3].etat").value("a-venir"))
                .andExpect(jsonPath("$.suivi[3].texte").value("Non atteinte"));
    }

    @Test
    void etapesDuWorkflow() throws Exception {
        mvc.perform(get("/api/demandes/etapes").with(user("manager")))
                .andExpect(jsonPath("$[*].libelle").value(org.hamcrest.Matchers.contains("Manager", "HR", "Direction", "Finance")))
                .andExpect(jsonPath("$[0].etat").value("courante"));
    }

    @Test
    void referentiel() throws Exception {
        mvc.perform(get("/api/referentiel/departements").with(user("manager")))
                .andExpect(jsonPath("$[*].label", hasItem("Informatique")));
        mvc.perform(get("/api/referentiel/fonctions").with(user("manager")))
                .andExpect(jsonPath("$[*].label", hasItem("Développeur")));
        mvc.perform(get("/api/referentiel/postes").param("departement", "2").with(user("manager")))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.containsInAnyOrder(3, 4)));
    }

    @Test
    void sansSessionRenvoie401() throws Exception {
        mvc.perform(get("/api/demandes/mes-demandes")).andExpect(status().isUnauthorized());
    }
}
