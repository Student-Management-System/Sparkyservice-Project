package net.ssehub.sparkyservice.api.util;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Util class for dates. Through that, we can have a consistent date handling in the whole project.
 *
 * @author marcel
 */
public final class DateUtil {

    /**
     * Method transforms a LocalDate to {@link Date}.
     * 
     * @param date - Date which is requested to be in the java.util.Date format
     * @return Same date as the provided LocalDate
     */
    public @Nonnull static java.util.Date toUtilDate(LocalDate date) {
        return notNull(
            Optional.of(date)
            .map(d -> d.atStartOfDay(ZoneId.systemDefault()).toInstant())
            .map(java.util.Date::from)
            .get()
        );
    }


    /**
     * Converts date. 
     * 
     * @param date - Date which will be transformed to an Sql Date.
     * @return sql.Date with values from the given one
     */
    public @Nonnull static java.sql.Date toSqlDate(LocalDate date) {
        return notNull(
            java.sql.Date.valueOf(date)
        );
    }

    /**
     * Converts date.
     * 
     * @param date - Date which will be transformed to an LocalDate.
     * @return LocalDate with values from the given one
     */
    public @Nonnull static LocalDate toLocalDate(@Nonnull java.sql.Date date) {
        return notNull(date.toLocalDate());
    }

    /**
     * Converts date.
     * 
     * @param date - Date which will be transformed to an LocalDate.
     * @return util.Date with values from the given one
     */
    public @Nonnull static LocalDate toLocalDate(@Nonnull java.util.Date date) {
        return notNull(
            Optional.of(date)
            .map(d -> d.toInstant())
            .map(LocalDate::from)
            .get()
        );
    }
}
