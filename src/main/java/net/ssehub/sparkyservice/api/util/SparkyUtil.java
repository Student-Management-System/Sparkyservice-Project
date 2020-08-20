package net.ssehub.sparkyservice.api.util;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public static @Nonnull <T> List<T> toList(@Nonnull final Iterable<T> iterable) {
        var list = StreamSupport.stream(iterable.spliterator(), false)
                            .filter(Objects::nonNull).collect(Collectors.toList());
        return NullHelpers.notNull(list);
    }

    /**
     * Converts a date to a string in the format: MM/dd/yyyy HH:mm:ss
     * 
     * @param expDate Desired date
     * @return the desired date as String
     */
    public static @Nonnull String expirationDateAsString(@Nullable Date expDate) {
        return notNull(
            Optional.of("MM/dd/yyyy HH:mm:ss")
                .map(SimpleDateFormat::new)
                .map(dateFormat -> dateFormat.format(expDate))
                .get()
            );
    }
}
