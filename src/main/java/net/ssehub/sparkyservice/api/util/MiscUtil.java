package net.ssehub.sparkyservice.api.util;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

/**
 * A collection of small util methods.
 * 
 * @author marcel
 */
public class MiscUtil {

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
     * Converts a Map to a set based on their entrys.
     * 
     * @param <T> - Entrys of the map which will be the entrys of the list
     * @param map
     * @return Set represents the entrys of a map
     */
    public static @Nonnull <T> Set<T> toSet(@Nonnull final Map<?, T> map) {
        return notNull(
            map.entrySet()
            .stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet())
        );
    }
    
}
