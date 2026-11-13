package gestion.hr.referentiel.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import gestion.hr.referentiel.Model.Personne;

/** Personnes (table personne). Utilisé par le référentiel (détenteurs) et les dossiers (candidats). */
public interface PersonneRepository extends JpaRepository<Personne, Integer> {

    /** Personne qui occupe le poste (table emploi), vide si le poste est vacant. */
    @Query(value = """
            SELECT p.* FROM personne p
            JOIN emploi e ON e.idPersonne = p.id
            WHERE e.idPoste = :idPoste""", nativeQuery = true)
    Optional<Personne> detenteurDuPoste(Integer idPoste);

    List<Personne> findAllByOrderByNomAscPrenomAsc();

    /** Personnes qui ont cet e-mail (comparé sans casse ni espaces autour) : contrôle des doublons. */
    @Query("select p from Personne p where lower(trim(p.email)) = lower(trim(:email))")
    List<Personne> avecEmail(String email);

    /** Personnes nées ce jour-là : contrôle des doublons nom + prénom + naissance (comparés en Java, sans accents). */
    List<Personne> findByNaissance(LocalDate naissance);

    /**
     * Postes occupés (table emploi) avec leur département : une personne qui en a un est un employé.
     * Une personne qui occupe plusieurs postes a plusieurs lignes (le premier par id de poste est pris).
     */
    @Query(value = """
            SELECT e.idPersonne AS "idPersonne", p.label AS "poste", d.label AS "departement"
            FROM emploi e
            JOIN poste p ON p.id = e.idPoste
            JOIN departement d ON d.id = p.idDepartement
            WHERE e.idPersonne IS NOT NULL
            ORDER BY p.id""", nativeQuery = true)
    List<Emploi> emplois();

    /** Une ligne de emplois(). */
    interface Emploi {
        Integer getIdPersonne();

        String getPoste();

        String getDepartement();
    }
}
