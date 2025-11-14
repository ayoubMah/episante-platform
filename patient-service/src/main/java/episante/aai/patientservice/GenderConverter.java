package episante.aai.patientservice;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Patient.Gender, String> {
    @Override
    public String convertToDatabaseColumn(Patient.Gender gender) {
        return gender == null ? null : gender.name();
    }

    @Override
    public Patient.Gender convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Patient.Gender.valueOf(dbData);
    }
}
