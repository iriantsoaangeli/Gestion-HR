package gestion.hr.offre.Service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import gestion.hr.auth.Model.Utilisateur;
import gestion.hr.auth.Repository.UtilisateurRepository;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.offre.Dto.OffreResponse;
import gestion.hr.offre.Model.Offre;
import gestion.hr.offre.Repository.OffrePosteRepository;
import gestion.hr.referentiel.Model.Departement;
import gestion.hr.referentiel.Model.Poste;
import gestion.hr.referentiel.Repository.DepartementRepository;
import gestion.hr.referentiel.Repository.PosteRepository;

/**
 * Offre -> OffreResponse (utilisé par LectureOffre et PublicationOffre).
 * Le département de l'offre vient de DepartementOffre (même règle que la fiche d'un entretien).
 */
@Component
class ConversionOffre {

    private final OffrePosteRepository offrePostes;
    private final PosteRepository postes;
    private final DepartementRepository departements;
    private final UtilisateurRepository utilisateurs;
    private final DossierRepository dossiers;
    private final DepartementOffre departementOffre;

    ConversionOffre(OffrePosteRepository offrePostes, PosteRepository postes, DepartementRepository departements,
            UtilisateurRepository utilisateurs, DossierRepository dossiers, DepartementOffre departementOffre) {
        this.offrePostes = offrePostes;
        this.postes = postes;
        this.departements = departements;
        this.utilisateurs = utilisateurs;
        this.dossiers = dossiers;
        this.departementOffre = departementOffre;
    }

    OffreResponse enReponse(Offre o) {
        List<OffreResponse.PosteCouvert> couverts = offrePostes.findByIdOffreOrderByIdAsc(o.getId()).stream()
                .map(op -> postes.findById(op.getIdPoste()).orElse(null))
                .filter(Objects::nonNull)
                .map(p -> new OffreResponse.PosteCouvert(p.getId(), p.getCode(), p.getLabel()))
                .toList();
        Integer idDepartement = departementOffre.idDe(o);
        String departement = idDepartement == null ? null
                : departements.findById(idDepartement).map(Departement::getLabel).orElse(null);
        String responsable = postes.findById(o.getIdPosteResponsable()).map(Poste::getLabel).orElse(null);
        String emetteur = utilisateurs.findById(o.getIdEmetteur()).map(Utilisateur::getLogin).orElse(null);
        String statutLibelle = o.getStatus().getLibelle();
        int candidats = dossiers.findByIdOffre(o.getId()).size();

        return new OffreResponse(o.getId(), o.getIntitule(), idDepartement, departement, o.getIdPosteResponsable(),
                responsable, o.getDescription(), o.getProfil(), o.getStatus(), statutLibelle, o.estOuverte(),
                o.getDelai(), o.getIdDemande(), o.getIdEmetteur(), emetteur, couverts, candidats);
    }
}
