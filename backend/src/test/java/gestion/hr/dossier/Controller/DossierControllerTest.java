package gestion.hr.dossier.Controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Dossiers de candidature sur la base locale (base/init.sql + data.sql + demo.sql).
 * Chaque test est annulé à la fin (@Transactional) : la base n'est pas modifiée.
 * Les id ne sont pas écrits en dur : ils sont lus par intitulé d'offre et prénom (demo.sql).
 * Démo : offres « Comptable » et « Développeur » ouvertes, « Gestionnaire paie » close ;
 * Andry a un dossier pour Comptable et Développeur, Volana pour Comptable seulement ; Toky (employé) pour Développeur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private int comptable;
    private int developpeur;
    private int paie;
    private int volana;
    private int andry;

    @BeforeEach
    void ids() {
        comptable = offre("Comptable");
        developpeur = offre("Développeur");
        paie = offre("Gestionnaire paie");
        volana = personne("Volana");
        andry = personne("Andry");
    }

    private int offre(String intitule) {
        return jdbc.queryForObject("SELECT id FROM offre WHERE intitule = ?", Integer.class, intitule);
    }

    private int personne(String prenom) {
        return jdbc.queryForObject("SELECT id FROM personne WHERE prenom = ?", Integer.class, prenom);
    }

    private static MockMultipartFile fichier(String nom, byte[] contenu) {
        return new MockMultipartFile("fichiers", nom, "application/pdf", contenu);
    }

    private static MockMultipartFile fichier(String nom) {
        return fichier(nom, ("contenu de " + nom).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Dépôt par hr, avec CSRF, pour une offre et ces fichiers. Les champs s'ajoutent ensuite par req.param(...)
     * (param() renvoie le type parent, sans file() : on ne chaîne pas file() après param()).
     */
    private static MockMultipartHttpServletRequestBuilder depot(int idOffre, MockMultipartFile... fichiers) {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/dossiers");
        for (MockMultipartFile f : fichiers) {
            req.file(f);
        }
        req.param("idOffre", String.valueOf(idOffre)).with(user("hr")).with(csrf());
        return req;
    }

    /**
     * Ajoute une nouvelle personne valide (inconnue de la base). changes : paires champ, valeur qui remplacent
     * les valeurs par défaut (req.param() ajouterait une 2e valeur, que Spring joindrait par une virgule).
     */
    private static MockMultipartHttpServletRequestBuilder nouvelle(MockMultipartHttpServletRequestBuilder req,
            String... changes) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put("nom", "Rakotobe");
        champs.put("prenom", "Nomena");
        champs.put("naissance", "1999-05-20");
        champs.put("adresse", "Lot I A 1 Analakely, Antananarivo");
        champs.put("tel", "034 00 000 01");
        champs.put("email", "nomena.rakotobe@exemple.mg");
        for (int i = 0; i + 1 < changes.length; i += 2) {
            champs.put(changes[i], changes[i + 1]);
        }
        champs.forEach(req::param);
        return req;
    }

    private ResultActions envoyer(MockMultipartHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req);
    }

    // ---------- Dépôt ----------

    @Test
    void hrDeposeUnDossierPourUnePersonneDeLaBase() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("CV.pdf"), fichier("Lettre.pdf"));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idOffre").value(developpeur))
                .andExpect(jsonPath("$.idPersonne").value(volana))
                .andExpect(jsonPath("$.candidat").value("Volana Ramanantsoa"))
                .andExpect(jsonPath("$.employe").value(false))
                .andExpect(jsonPath("$.fichiers[0]").value("CV.pdf"))
                .andExpect(jsonPath("$.fichiers[1]").value("Lettre.pdf"))
                .andExpect(jsonPath("$.piecesJointes.length()").value(2))
                .andExpect(jsonPath("$.piecesJointes[0].type").value("application/pdf"))
                .andExpect(jsonPath("$.piecesJointes[0].taille").value("contenu de CV.pdf".length()));
    }

    @Test
    void hrDeposeUnDossierPourUneNouvellePersonne() throws Exception {
        MockMultipartHttpServletRequestBuilder req = nouvelle(depot(comptable, fichier("CV.pdf")));
        String json = envoyer(req)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidat").value("Nomena Rakotobe"))
                .andExpect(jsonPath("$.email").value("nomena.rakotobe@exemple.mg"))
                .andExpect(jsonPath("$.employe").value(false))
                .andReturn().getResponse().getContentAsString();
        int idPersonne = JsonPath.read(json, "$.idPersonne");

        mvc.perform(get("/api/dossiers/personnes").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].naissance".formatted(idPersonne)).value(hasItem("1999-05-20")));
    }

    @Test
    void personneEnDoubleParEmailRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = nouvelle(depot(developpeur, fichier("CV.pdf")),
                "email", "  ANDRY.rakotoarisoa@exemple.mg ");
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(containsString("existe déjà (Andry Rakotoarisoa")));
    }

    @Test
    void personneEnDoubleParNomPrenomNaissanceRefusee() throws Exception {
        // Éric Randriamampionona, 1991-10-19 : retrouvé sans accent ni majuscules
        MockMultipartHttpServletRequestBuilder req = nouvelle(depot(developpeur, fichier("CV.pdf")),
                "nom", "randriamampionona", "prenom", "ERIC", "naissance", "1991-10-19");
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("existe déjà")));
    }

    @Test
    void personneExistanteEtNouvelleEnMemeTempsRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = nouvelle(depot(developpeur, fichier("CV.pdf")));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test
    void nouvellePersonneIncompleteRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("CV.pdf"));
        req.param("nom", "Seul");
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le prénom est obligatoire."));
    }

    @Test
    void naissanceDansLeFuturRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = nouvelle(depot(developpeur, fichier("CV.pdf")),
                "naissance", "2999-01-01");
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La date de naissance doit être dans le passé."));
    }

    @Test
    void dossierEnDoubleRefuse() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(comptable, fichier("CV.pdf"));
        req.param("idPersonne", String.valueOf(andry));
        envoyer(req)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_FAIT"))
                .andExpect(jsonPath("$.message").value("Andry Rakotoarisoa a déjà déposé un dossier pour cette offre."));
    }

    @Test
    void offreCloseRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(paie, fichier("CV.pdf"));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("est close")));
    }

    @Test
    void offreInconnueRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(999999, fichier("CV.pdf"));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Offre inconnue."));
    }

    @Test
    void personneInconnueRefusee() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("CV.pdf"));
        req.param("idPersonne", "abc");
        envoyer(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Personne inconnue."));
    }

    @Test
    void sansFichierRefuse() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur);
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Joignez au moins un fichier (CV, lettre de motivation…)."));
    }

    @Test
    void fichierVideRefuse() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("vide.pdf", new byte[0]));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le fichier « vide.pdf » est vide."));
    }

    @Test
    void plusDe30MoRefuse() throws Exception {
        // MockMvc n'applique pas la limite multipart de Spring : c'est le service qui refuse
        MockMultipartHttpServletRequestBuilder req = depot(developpeur,
                fichier("gros1.pdf", new byte[20 * 1024 * 1024]), fichier("gros2.pdf", new byte[10 * 1024 * 1024 + 1]));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("dépassent 30 Mo")));
    }

    @Test
    void memeFichierDeuxFoisRefuse() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("CV.pdf"), fichier("cv.PDF"));
        req.param("idPersonne", String.valueOf(volana));
        envoyer(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("deux fois")));
    }

    @Test
    void seulHrPeutDeposer() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/dossiers").file(fichier("CV.pdf"));
        req.param("idOffre", String.valueOf(developpeur)).param("idPersonne", String.valueOf(volana))
                .with(user("manager")).with(csrf());
        envoyer(req).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void depotSansCsrfRefuse() throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/dossiers").file(fichier("CV.pdf"));
        req.param("idOffre", String.valueOf(developpeur)).param("idPersonne", String.valueOf(volana))
                .with(user("hr"));
        envoyer(req).andExpect(status().isForbidden());
    }

    // ---------- Lecture ----------

    @Test
    void hrListeTousLesDossiers() throws Exception {
        mvc.perform(get("/api/dossiers").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$[0].candidat").isString())
                .andExpect(jsonPath("$[0].fichiers").isArray());
    }

    @Test
    void dossiersDUneOffre() throws Exception {
        mvc.perform(get("/api/dossiers").param("offre", String.valueOf(developpeur)).with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idOffre").value(everyItem(is(developpeur))))
                // Toky occupe un poste : candidature interne
                .andExpect(jsonPath("$[?(@.candidat == 'Toky Rabearivelo')].employe").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.candidat == 'Andry Rakotoarisoa')].fichiers[*]").value(hasItem("Lettre.txt")));
    }

    @Test
    void personnesAvecLeurPoste() throws Exception {
        mvc.perform(get("/api/dossiers/personnes").with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.prenom == 'Hery')].poste").value(hasItem("Manager informatique")))
                .andExpect(jsonPath("$[?(@.prenom == 'Hery')].departement").value(hasItem("Informatique")))
                .andExpect(jsonPath("$[?(@.prenom == 'Andry')].poste").value(hasItem(nullValue())));
    }

    @Test
    void seulHrVoitLesDossiers() throws Exception {
        mvc.perform(get("/api/dossiers").with(user("manager"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/dossiers/personnes").with(user("direction"))).andExpect(status().isForbidden());
    }

    @Test
    void telechargerUnePieceJointe() throws Exception {
        MockMultipartHttpServletRequestBuilder req = depot(developpeur, fichier("cv.pdf"));
        req.param("idPersonne", String.valueOf(volana));
        String json = envoyer(req).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPiece = JsonPath.read(json, "$.piecesJointes[0].id");

        mvc.perform(get("/api/dossiers/pieces-jointes/" + idPiece).with(user("hr")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("cv.pdf")))
                .andExpect(content().bytes("contenu de cv.pdf".getBytes(StandardCharsets.UTF_8)));

        mvc.perform(get("/api/dossiers/pieces-jointes/" + idPiece).with(user("manager")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pieceJointeInconnue() throws Exception {
        mvc.perform(get("/api/dossiers/pieces-jointes/999999").with(user("hr")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"));
    }
}
