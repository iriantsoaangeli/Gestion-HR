package gestion.hr.demande.Model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Colonne idStatus (entier, clé vers status) <-> enum Statut. */
@Converter
public class StatutConverter implements AttributeConverter<Statut, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Statut statut) {
        return statut == null ? null : statut.getId();
    }

    @Override
    public Statut convertToEntityAttribute(Integer id) {
        return id == null ? null : Statut.deId(id);
    }
}
