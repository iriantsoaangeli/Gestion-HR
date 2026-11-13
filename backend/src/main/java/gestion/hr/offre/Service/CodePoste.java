package gestion.hr.offre.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Code lisible d'un nouveau poste : <code du département>-<année>-<compteur sur 4 chiffres>, ex. INF-2026-0007.
 * Un compteur par département et par année, à la suite du plus grand compteur déjà en base pour ce préfixe
 * (les postes de data.sql et demo.sql comptent). Deux publications simultanées pourraient tirer le même code :
 * la contrainte UNIQUE de poste.code refuse alors la seconde.
 */
@Component
class CodePoste {

    private final PosteRepository postes;

    CodePoste(PosteRepository postes) {
        this.postes = postes;
    }

    /** Les {@code nombre} codes suivants pour ce département, cette année. */
    List<String> suivants(String codeDepartement, int nombre) {
        String prefixe = codeDepartement + "-" + LocalDate.now().getYear() + "-";
        int dernier = 0;
        for (Poste p : postes.findByCodeStartingWith(prefixe)) {
            dernier = Math.max(dernier, compteur(p.getCode().substring(prefixe.length())));
        }
        List<String> codes = new ArrayList<>();
        for (int k = 1; k <= nombre; k++) {
            codes.add(prefixe + String.format("%04d", dernier + k));
        }
        return codes;
    }

    /** Compteur écrit dans un code (0 s'il n'est pas un nombre, ex. un code saisi à la main). */
    private static int compteur(String texte) {
        try {
            return Integer.parseInt(texte);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
