package gestion.hr.dossier.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import gestion.hr.commun.Exception.ErreurMetier;
import gestion.hr.dossier.Dto.DossierResponse;
import gestion.hr.dossier.Dto.FichierResponse;
import gestion.hr.dossier.Dto.PersonneResponse;
import gestion.hr.dossier.Model.Dossier;
import gestion.hr.dossier.Model.PieceJointe;
import gestion.hr.dossier.Repository.DossierRepository;
import gestion.hr.dossier.Repository.PieceDuDossier;
import gestion.hr.dossier.Repository.PieceJointeRepository;
import gestion.hr.referentiel.Model.Personne;
import gestion.hr.referentiel.Repository.PersonneRepository;

/**
 * Lectures des dossiers (appelé seulement par DossierService, qui porte la transaction) :
 * personnes candidates, dossiers (tous ou d'une offre) avec candidat et pièces jointes, téléchargement.
 * Les jointures sont faites ici en quelques requêtes (pas une par dossier) ; le contenu des fichiers
 * n'est chargé que pour un téléchargement.
 */
@Component
class LectureDossier {

    private final DossierRepository dossiers;
    private final PieceJointeRepository pieces;
    private final PersonneRepository personnes;

    LectureDossier(DossierRepository dossiers, PieceJointeRepository pieces, PersonneRepository personnes) {
        this.dossiers = dossiers;
        this.pieces = pieces;
        this.personnes = personnes;
    }

    /** Toutes les personnes, triées par nom puis prénom, avec leur poste s'il y en a un. */
    List<PersonneResponse> personnes() {
        Map<Integer, PersonneRepository.Emploi> emplois = emplois();
        return personnes.findAllByOrderByNomAscPrenomAsc().stream().map(p -> {
            PersonneRepository.Emploi e = emplois.get(p.getId());
            return new PersonneResponse(p.getId(), p.getNom(), p.getPrenom(), p.getNaissance(), p.getAdresse(),
                    p.getTel(), p.getEmail(), e == null ? null : e.getPoste(), e == null ? null : e.getDepartement());
        }).toList();
    }

    /** Dossiers d'une offre (idOffre non null) ou tous, par id croissant. */
    List<DossierResponse> dossiers(Integer idOffre) {
        return enReponses(idOffre == null
                ? dossiers.findAllByOrderByIdAsc()
                : dossiers.findByIdOffreOrderByIdAsc(idOffre));
    }

    DossierResponse enReponse(Dossier d) {
        return enReponses(List.of(d)).get(0);
    }

    FichierResponse fichier(Integer id) {
        PieceJointe p = (id == null ? null : pieces.findById(id).orElse(null));
        if (p == null) {
            throw ErreurMetier.introuvable("Pièce jointe introuvable.");
        }
        return new FichierResponse(p.getNom(), typeValide(p.getType()), p.getContenu());
    }

    /** Type MIME utilisable dans Content-Type, sinon application/octet-stream (type vide ou mal formé). */
    static String typeValide(String type) {
        if (type == null || type.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        try {
            MediaType.parseMediaType(type);
            return type.trim();
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private List<DossierResponse> enReponses(List<Dossier> liste) {
        if (liste.isEmpty()) {
            return List.of();
        }
        List<Integer> idsPersonnes = liste.stream().map(Dossier::getIdPersonne).distinct().toList();
        Map<Integer, Personne> candidats = personnes.findAllById(idsPersonnes).stream()
                .collect(Collectors.toMap(Personne::getId, Function.identity()));
        Map<Integer, PersonneRepository.Emploi> emplois = emplois();
        Map<Integer, List<DossierResponse.PieceJointeResponse>> piecesParDossier =
                piecesDe(liste.stream().map(Dossier::getId).toList());

        return liste.stream().map(d -> {
            Personne p = candidats.get(d.getIdPersonne());
            List<DossierResponse.PieceJointeResponse> pj = piecesParDossier.getOrDefault(d.getId(), List.of());
            return new DossierResponse(d.getId(), d.getDate(), d.getIdOffre(), d.getIdPersonne(),
                    p.getNomComplet(), p.getEmail(), p.getTel(), emplois.containsKey(p.getId()),
                    pj.stream().map(DossierResponse.PieceJointeResponse::nom).toList(), pj);
        }).toList();
    }

    private Map<Integer, List<DossierResponse.PieceJointeResponse>> piecesDe(Collection<Integer> idsDossiers) {
        Map<Integer, List<DossierResponse.PieceJointeResponse>> resultat = new HashMap<>();
        for (PieceDuDossier l : pieces.desDossiers(idsDossiers)) {
            resultat.computeIfAbsent(l.idDossier(), k -> new ArrayList<>())
                    .add(new DossierResponse.PieceJointeResponse(l.id(), l.nom(), l.type(), l.taille()));
        }
        return resultat;
    }

    /** Premier poste occupé par chaque personne employée (idPersonne → poste + département). */
    private Map<Integer, PersonneRepository.Emploi> emplois() {
        Map<Integer, PersonneRepository.Emploi> resultat = new HashMap<>();
        for (PersonneRepository.Emploi e : personnes.emplois()) {
            resultat.putIfAbsent(e.getIdPersonne(), e);
        }
        return resultat;
    }
}
