package gestion.hr.offre.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gestion.hr.offre.Dto.DemandeApprouveeResponse;
import gestion.hr.offre.Dto.OffreResponse;
import gestion.hr.offre.Dto.PublierOffreRequest;

/**
 * Point d'entrée unique des offres d'emploi (tout est réservé à HR).
 * Chaque section est dans sa classe du même package, invisible hors de celui-ci :
 * - PublicationOffre  : publier une offre depuis une ligne d'une demande approuvée (crée les postes vides)
 * - LectureOffre      : liste, fiche, demandes approuvées pour le formulaire
 * - CouvertureDemande : quelles lignes d'une demande ont déjà une offre
 * - CodePoste         : code lisible des nouveaux postes (INF-2026-0007)
 * - ConversionOffre   : Offre -> OffreResponse (libellés, postes couverts, candidats)
 * - AccesOffre        : contrôle « HR seulement »
 * - DepartementOffre  : département d'une offre (public : aussi utilisé par les entretiens)
 * Les transactions sont ouvertes ici.
 */
@Service
public class OffreService {

    private final PublicationOffre publication;
    private final LectureOffre lecture;

    OffreService(PublicationOffre publication, LectureOffre lecture) {
        this.publication = publication;
        this.lecture = lecture;
    }

    @Transactional
    public OffreResponse publier(PublierOffreRequest req) {
        return publication.publier(req);
    }

    @Transactional(readOnly = true)
    public List<OffreResponse> offres() {
        return lecture.offres();
    }

    @Transactional(readOnly = true)
    public OffreResponse offre(Integer id) {
        return lecture.offre(id);
    }

    @Transactional(readOnly = true)
    public List<DemandeApprouveeResponse> demandesApprouvees() {
        return lecture.demandesApprouvees();
    }
}
