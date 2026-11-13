package gestion.hr.offre.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import gestion.hr.demande.Dto.DemandeResponse;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Model.OffrePoste;
import gestion.hr.offre.Repository.FonctionPosteRepository;
import gestion.hr.offre.Repository.OffrePosteRepository;
import gestion.hr.offre.Repository.OffreRepository;

/**
 * Quelles lignes d'une demande ont déjà une offre (ouverte ou close) : une ligne n'est publiée qu'une fois.
 * - Remplacement couvert : une offre de la demande couvre ce poste.
 * - Création couverte : une offre de la demande couvre un poste (pas un poste à remplacer) qui a cette fonction
 *   (fonctionPoste, écrite à la création des postes vides).
 */
@Component
class CouvertureDemande {

    /** idFonction (création) ou idPoste (remplacement) -> id de l'offre qui couvre la ligne. */
    record Couverture(Map<Integer, Integer> creations, Map<Integer, Integer> remplacements) {
    }

    private final OffreRepository offres;
    private final OffrePosteRepository offrePostes;
    private final FonctionPosteRepository fonctionsPostes;

    CouvertureDemande(OffreRepository offres, OffrePosteRepository offrePostes,
            FonctionPosteRepository fonctionsPostes) {
        this.offres = offres;
        this.offrePostes = offrePostes;
        this.fonctionsPostes = fonctionsPostes;
    }

    Couverture de(DemandeResponse d) {
        Set<Integer> aRemplacer = d.remplacements().stream().map(DemandeResponse.Remplacement::idPoste)
                .collect(Collectors.toSet());
        Map<Integer, Integer> creations = new HashMap<>();
        Map<Integer, Integer> remplacements = new HashMap<>();
        for (Offre o : offres.findByIdDemande(d.id())) {
            for (OffrePoste op : offrePostes.findByIdOffreOrderByIdAsc(o.getId())) {
                if (aRemplacer.contains(op.getIdPoste())) {
                    remplacements.putIfAbsent(op.getIdPoste(), o.getId());
                } else {
                    fonctionsPostes.findFirstByIdPoste(op.getIdPoste())
                            .ifPresent(f -> creations.putIfAbsent(f.getIdFonction(), o.getId()));
                }
            }
        }
        return new Couverture(creations, remplacements);
    }
}
