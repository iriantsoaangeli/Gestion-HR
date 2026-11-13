package gestion.hr.entretien.Model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Colonne idStatusEntretien (entier, clé vers statusEntretien) <-> enum StatutEntretien. */
@Converter
public class StatutEntretienConverter implements AttributeConverter<StatutEntretien, Integer> {

    @Override
    public Integer convertToDatabaseColumn(StatutEntretien statut) {
        return statut == null ? null : statut.getId();
    }

    @Override
    public StatutEntretien convertToEntityAttribute(Integer id) {
        return id == null ? null : StatutEntretien.deId(id);
    }
}
