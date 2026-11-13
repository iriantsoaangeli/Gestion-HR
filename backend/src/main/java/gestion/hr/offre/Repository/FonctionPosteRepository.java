package gestion.hr.offre.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.offre.Model.FonctionPoste;

/** Fonction d'un poste (table fonctionPoste). */
public interface FonctionPosteRepository extends JpaRepository<FonctionPoste, Integer> {

    /** La fonction d'un poste (un poste créé par une offre en a une seule). */
    Optional<FonctionPoste> findFirstByIdPoste(Integer idPoste);
}
