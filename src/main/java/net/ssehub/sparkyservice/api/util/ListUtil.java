package net.ssehub.sparkyservice.api.util;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * Util class for dealing with list.
 * 
 * @author marcel
 */
public class ListUtil {

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
}
