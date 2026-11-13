package gestion.hr.offre.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gestion.hr.offre.Model.CreationPoste;

/** Dates de création des postes (table creationPoste). */
public interface CreationPosteRepository extends JpaRepository<CreationPoste, Integer> {
}
