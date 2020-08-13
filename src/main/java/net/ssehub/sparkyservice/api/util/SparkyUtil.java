package net.ssehub.sparkyservice.api.util;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * A collection of small util methods.
 * 
 * @author marcel
 */
public class SparkyUtil {

    /**
     * Converts an iterable to List of the same type. 
     * 
     * @param <T> - Type of the generics
     * @param iterable 
     * @return A list with the same objects as iterable
     */
    public static @Nonnull <T> List<T> toList(final Iterable<T> iterable) {
        var list = StreamSupport.stream(iterable.spliterator(), false)
                            .filter(Objects::nonNull).collect(Collectors.toList());
        return NullHelpers.notNull(list);
    }

    /**
     * Method transforms a LocalDate to {@link Date}.
     * 
     * @param date - Date which is requested to be in the java.util.Date format
     * @return Same date as the provided LocalDate
     */
    public @Nonnull static java.util.Date toJavaUtilDate(LocalDate date) {
        return notNull(
            Optional.of(date)
            .map(d -> d.atStartOfDay(ZoneId.systemDefault()).toInstant())
            .map(java.util.Date::from)
            .get()
        );
    }
}
