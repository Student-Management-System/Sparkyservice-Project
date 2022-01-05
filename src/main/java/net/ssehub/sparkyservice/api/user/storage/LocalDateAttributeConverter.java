package net.ssehub.sparkyservice.api.user.storage;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts a {@link LocalDate} attribute of an {@link javax.persistence.Entity} to a {@link Date} object for
 * persistence and vice versa.
 * @see <a href="https://thorben-janssen.com/persist-localdate-localdatetime-jpa/">Code Source</a>.
 * @author El-Sharkawy
 *
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {
     
    @Override
    public Date convertToDatabaseColumn(LocalDate locDate) {
        return locDate == null ? null : Date.valueOf(locDate);
    }
 
    @Override
    public LocalDate convertToEntityAttribute(Date sqlDate) {
        return sqlDate == null ? null : sqlDate.toLocalDate();
    }
}