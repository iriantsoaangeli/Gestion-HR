package gestion.hr.offre.Service;

import org.springframework.stereotype.Component;

import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Repository.OffrePosteRepository;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Département d'une offre, seule règle utilisée partout (liste des offres, fiche d'un entretien) :
 * celui de son premier poste couvert (offrePoste, tous dans le département de la demande),
 * à défaut celui du poste responsable.
 */
@Component
public class DepartementOffre {

    private final OffrePosteRepository offrePostes;
    private final PosteRepository postes;

    DepartementOffre(OffrePosteRepository offrePostes, PosteRepository postes) {
        this.offrePostes = offrePostes;
        this.postes = postes;
    }

    /** id du département de l'offre (null si aucun poste n'est trouvé). */
    public Integer idDe(Offre o) {
        Integer idPoste = offrePostes.findByIdOffreOrderByIdAsc(o.getId()).stream().map(x -> x.getIdPoste())
                .findFirst().orElse(o.getIdPosteResponsable());
        return idPoste == null ? null : postes.findById(idPoste).map(Poste::getIdDepartement).orElse(null);
    }
}
