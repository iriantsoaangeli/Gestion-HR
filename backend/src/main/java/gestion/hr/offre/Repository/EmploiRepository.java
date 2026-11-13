package gestion.hr.offre.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.offre.Model.Emploi;

/** Emplois (table emploi) : ici seulement pour créer l'emploi vacant d'un nouveau poste. */
public interface EmploiRepository extends JpaRepository<Emploi, Integer> {
}
