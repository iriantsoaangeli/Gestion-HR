package gestion.hr.entretien.Controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
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

/**
 * Entretiens et évaluations sur la base locale (base/init.sql + data.sql + demo.sql).
 * Chaque test est annulé à la fin (@Transactional) : la base n'est pas modifiée.
 * Les id sont relus en SQL (prénom du candidat, intitulé de l'offre, libellé du poste) : ils ne dépendent pas
 * de l'ordre d'insertion. Démo : Haja (Développeur) a un entretien en attente, interviewers manager + hr,
 * seul hr a évalué (70) ; Andry (Développeur) est fini (85 et 80) ; l'offre « Gestionnaire paie » est close.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntretienControllerTest {

    private static final String PAS_INTERVIEWER = "Vous n'êtes pas interviewer de cet entretien.";
    private static final String DEJA_EVALUE = "Vous avez déjà évalué cet entretien.";
    private static final String A_VENIR = "L'entretien n'a pas encore eu lieu : vous pourrez l'évaluer après sa date.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate sql;

    @Autowired
    private EntityManager em;

    private int posteRh;
    private int posteInfo;
    private int entretienHaja;
    private int entretienAndry;

    @BeforeEach
    void lireLesId() {
        posteRh = poste("Responsable RH");
        posteInfo = poste("Manager informatique");
        entretienHaja = entretien("Haja", "Développeur");
        entretienAndry = entretien("Andry", "Développeur");
    }

    // ---------- Outils ----------

    private int poste(String label) {
        return sql.queryForObject("SELECT min(id) FROM poste WHERE label = ?", Integer.class, label);
    }

    private int dossier(String prenom, String offre) {
        return sql.queryForObject("""
                SELECT d.id FROM dossier d JOIN personne p ON p.id = d.idPersonne JOIN offre o ON o.id = d.idOffre
                WHERE p.prenom = ? AND o.intitule = ?""", Integer.class, prenom, offre);
    }

    private int entretien(String prenom, String offre) {
        return sql.queryForObject("SELECT min(id) FROM entretien WHERE idDossier = ?", Integer.class,
                dossier(prenom, offre));
    }

    private int evaluation(int idEntretien, int idPoste) {
        return sql.queryForObject("SELECT id FROM evaluation WHERE idEntretien = ? AND idPoste = ?", Integer.class,
                idEntretien, idPoste);
    }

    private static MockHttpServletRequestBuilder en(String login, MockHttpServletRequestBuilder req) {
        return req.with(user(login)).with(csrf());
    }

    private ResultActions lire(String login, String url) throws Exception {
        return mvc.perform(get(url).with(user(login)));
    }

    private ResultActions preparer(String login, String json) throws Exception {
        return mvc.perform(en(login, post("/api/entretiens")).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions evaluer(String login, int id, String json) throws Exception {
        return mvc.perform(en(login, post("/api/entretiens/" + id + "/evaluation"))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Corps de préparation : dossier de Tiana pour l'offre « Comptable » (ouverte, sans entretien). */
    private String corps(String date, String interviewers) {
        return """
                {"idDossier": %d, "date": "%s", "interviewers": %s}"""
                .formatted(dossier("Tiana", "Comptable"), date, interviewers);
    }

    private static String dansDixJours() {
        return LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0).toString();
    }

    // ---------- Préparer ----------

    @Test
    void hrPrepareUnEntretienEnAttente() throws Exception {
        preparer("hr", corps(dansDixJours(), "[%d, %d]".formatted(posteRh, posteInfo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.statutLibelle").value("En attente"))
                .andExpect(jsonPath("$.candidat").value("Tiana Rasolofonirina"))
                .andExpect(jsonPath("$.poste").value("Comptable"))
                .andExpect(jsonPath("$.departement").value("Finance"))
                .andExpect(jsonPath("$.interviewers.length()").value(2))
                .andExpect(jsonPath("$.interviewers[0].detenteur").value("Lalao Rasoanaivo"))
                .andExpect(jsonPath("$.evaluations").isEmpty())
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.monPoste").value(posteRh))
                .andExpect(jsonPath("$.peutEvaluer").value(false))
                .andExpect(jsonPath("$.raisonEvaluer").value(A_VENIR));
    }

    @Test
    void unCandidatPeutAvoirPlusieursEntretiens() throws Exception {
        preparer("hr", """
                {"idDossier": %d, "date": "%s", "interviewers": [%d]}"""
                .formatted(dossier("Haja", "Développeur"), dansDixJours(), posteInfo))
                .andExpect(status().isCreated());
    }

    @Test
    void seulHrPrepare() throws Exception {
        preparer("manager", corps(dansDixJours(), "[%d]".formatted(posteRh)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void datePasseeRefusee() throws Exception {
        preparer("hr", corps(LocalDateTime.now().minusMinutes(1).withNano(0).toString(), "[%d]".formatted(posteRh)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date de l'entretien doit être dans le futur."));
    }

    @Test
    void offreCloseRefusee() throws Exception {
        preparer("hr", """
                {"idDossier": %d, "date": "%s", "interviewers": [%d]}"""
                .formatted(dossier("Éric", "Gestionnaire paie"), dansDixJours(), posteRh))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void dossierInconnuRefuse() throws Exception {
        preparer("hr", """
                {"idDossier": 999999, "date": "%s", "interviewers": [%d]}""".formatted(dansDixJours(), posteRh))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void interviewersRefuses() throws Exception {
        // aucun, poste de Finance, poste vacant sans compte, doublon
        for (String liste : new String[] { "[]", "[" + poste("Directeur financier") + "]", "[" + poste("Développeur") + "]",
                "[%d, %d]".formatted(posteRh, posteRh), "[null]" }) {
            preparer("hr", corps(dansDixJours(), liste))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }
    }

    @Test
    void interviewersPossiblesSansFinanceNiAdmin() throws Exception {
        lire("hr", "/api/entretiens/interviewers-possibles")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idPoste", hasItem(posteRh)))
                .andExpect(jsonPath("$[*].idPoste", hasItem(posteInfo)))
                .andExpect(jsonPath("$[*].idPoste", hasItem(poste("Directeur général"))))
                .andExpect(jsonPath("$[*].idPoste", not(hasItem(poste("Directeur financier")))))
                .andExpect(jsonPath("$[*].idPoste", not(hasItem(poste("Administrateur de l'application")))))
                .andExpect(jsonPath("$[*].idPoste", not(hasItem(poste("Développeur")))));
        lire("manager", "/api/entretiens/interviewers-possibles").andExpect(status().isForbidden());
    }

    // ---------- Lire, notes séparées ----------

    @Test
    void notesCacheesAUnInterviewerQuiNAPasEvalue() throws Exception {
        lire("manager", "/api/entretiens/" + entretienHaja)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notesCachees").value(true))
                .andExpect(jsonPath("$.note").doesNotExist())
                .andExpect(jsonPath("$.evaluations.length()").value(1))
                .andExpect(jsonPath("$.evaluations[0].detenteur").value("Lalao Rasoanaivo"))
                .andExpect(jsonPath("$.evaluations[0].note").doesNotExist())
                .andExpect(jsonPath("$.evaluations[0].remarques").doesNotExist())
                .andExpect(jsonPath("$.monPoste").value(posteInfo))
                .andExpect(jsonPath("$.peutEvaluer").value(true));
    }

    @Test
    void notesVisiblesApresSonEvaluationEtPourLesAutres() throws Exception {
        lire("hr", "/api/entretiens/" + entretienHaja)
                .andExpect(jsonPath("$.notesCachees").value(false))
                .andExpect(jsonPath("$.note").value(70))
                .andExpect(jsonPath("$.maNote").value(70))
                .andExpect(jsonPath("$.peutEvaluer").value(false))
                .andExpect(jsonPath("$.raisonEvaluer").value(DEJA_EVALUE));
        lire("direction", "/api/entretiens/" + entretienHaja)
                .andExpect(jsonPath("$.notesCachees").value(false))
                .andExpect(jsonPath("$.evaluations[0].remarques").value("Bon contact, connaît bien Linux."))
                .andExpect(jsonPath("$.monPoste").doesNotExist())
                .andExpect(jsonPath("$.raisonEvaluer").value(PAS_INTERVIEWER));
    }

    @Test
    void listesParStatutEtParOffre() throws Exception {
        lire("direction", "/api/entretiens/en-attente")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].statut", everyItem(is("EN_ATTENTE"))))
                .andExpect(jsonPath("$[*].id", hasItem(entretienHaja)));
        lire("direction", "/api/entretiens/finis")
                .andExpect(jsonPath("$[*].statut", everyItem(is("FINI"))))
                .andExpect(jsonPath("$[*].id", hasItem(entretienAndry)));
        int offreDev = sql.queryForObject("SELECT id FROM offre WHERE intitule = 'Développeur'", Integer.class);
        lire("hr", "/api/entretiens?offre=" + offreDev)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].idOffre", everyItem(is(offreDev))));
        lire("hr", "/api/entretiens?offre=999999").andExpect(status().isNotFound());
        lire("hr", "/api/entretiens/999999").andExpect(status().isNotFound());
    }

    @Test
    void financeEtAdminNOntPasAcces() throws Exception {
        lire("finance", "/api/entretiens/en-attente")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
        lire("admin", "/api/evaluations/a-faire").andExpect(status().isForbidden());
    }

    // ---------- Évaluer ----------

    @Test
    void derniereEvaluationFinitLEntretien() throws Exception {
        evaluer("manager", entretienHaja, """
                {"note": 90, "remarques": "  Solide en Linux.  "}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("FINI"))
                .andExpect(jsonPath("$.fini").value(true))
                .andExpect(jsonPath("$.note").value(80))
                .andExpect(jsonPath("$.maNote").value(90))
                .andExpect(jsonPath("$.idEvaluation").isNumber())
                .andExpect(jsonPath("$.notesCachees").value(false))
                .andExpect(jsonPath("$.peutEvaluer").value(false));
        evaluer("manager", entretienHaja, """
                {"note": 50, "remarques": "Deuxième fois"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_FAIT"))
                .andExpect(jsonPath("$.message").value(DEJA_EVALUE));
    }

    /** Prépare un entretien dans 10 jours (hr + manager) et renvoie son id. */
    private int nouvelEntretien() throws Exception {
        String json = preparer("hr", corps(dansDixJours(), "[%d, %d]".formatted(posteRh, posteInfo)))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.id");
    }

    @Test
    void pasDEvaluationAvantLaDate() throws Exception {
        int id = nouvelEntretien();
        lire("hr", "/api/entretiens/" + id)
                .andExpect(jsonPath("$.peutEvaluer").value(false))
                .andExpect(jsonPath("$.raisonEvaluer").value(A_VENIR));
        evaluer("hr", id, """
                {"note": 50, "remarques": "Trop tôt"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENTRETIEN_A_VENIR"))
                .andExpect(jsonPath("$.message").value(A_VENIR));
    }

    @Test
    void premiereEvaluationLaisseEnAttente() throws Exception {
        int id = nouvelEntretien();
        // L'entretien a eu lieu (la préparation n'accepte qu'une date future)
        em.flush();
        sql.update("UPDATE entretien SET date = now() - interval '1 hour' WHERE id = ?", id);
        em.clear();
        evaluer("hr", id, """
                {"note": 0, "remarques": "Absent"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.note").value(0));
    }

    @Test
    void lePosteVientDeLaSessionPasDuJson() throws Exception {
        // hr a déjà évalué : envoyer le poste du manager ne change rien
        evaluer("hr", entretienHaja, """
                {"idPoste": %d, "note": 60, "remarques": "Je me fais passer pour le manager"}""".formatted(posteInfo))
                .andExpect(status().isConflict());
    }

    @Test
    void seulUnInterviewerEvalue() throws Exception {
        evaluer("direction", entretienHaja, """
                {"note": 60, "remarques": "x"}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(PAS_INTERVIEWER));
    }

    @Test
    void noteEtRemarquesInvalides() throws Exception {
        for (String json : new String[] {
                "{\"note\": 101, \"remarques\": \"x\"}",
                "{\"note\": -1, \"remarques\": \"x\"}",
                "{\"note\": 50.5, \"remarques\": \"x\"}",
                "{\"note\": null, \"remarques\": \"x\"}",
                "{\"note\": 50, \"remarques\": \"   \"}",
                "{\"note\": 50}" }) {
            evaluer("manager", entretienHaja, json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION"));
        }
    }

    @Test
    void evaluerUnEntretienInconnu() throws Exception {
        evaluer("hr", 999999, "{\"note\": 50, \"remarques\": \"x\"}").andExpect(status().isNotFound());
    }

    // ---------- Mes évaluations ----------

    @Test
    void mesEvaluationsAFaireEtFaites() throws Exception {
        lire("manager", "/api/evaluations/a-faire")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(entretienHaja)))
                .andExpect(jsonPath("$[*].id", not(hasItem(entretienAndry))));
        lire("manager", "/api/evaluations/faites")
                .andExpect(jsonPath("$[*].id", hasItem(entretienAndry)))
                .andExpect(jsonPath("$[?(@.id == %d)].maNote".formatted(entretienAndry), hasItem(85)))
                .andExpect(jsonPath("$[?(@.id == %d)].idEvaluation".formatted(entretienAndry),
                        hasItem(evaluation(entretienAndry, posteInfo))));
        lire("direction", "/api/evaluations/a-faire").andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void ficheEvaluationRefuseeTantQueLesNotesSontCachees() throws Exception {
        int id = evaluation(entretienHaja, posteRh);
        lire("manager", "/api/evaluations/" + id)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
        lire("hr", "/api/evaluations/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value(70))
                .andExpect(jsonPath("$.deMoi").value(true))
                .andExpect(jsonPath("$.entretien.id").value(entretienHaja));
        lire("direction", "/api/evaluations/" + id)
                .andExpect(jsonPath("$.deMoi").value(false))
                .andExpect(jsonPath("$.remarques").value("Bon contact, connaît bien Linux."));
        lire("hr", "/api/evaluations/999999").andExpect(status().isNotFound());
    }
}
