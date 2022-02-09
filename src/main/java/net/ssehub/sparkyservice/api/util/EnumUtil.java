package net.ssehub.sparkyservice.api.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

/**
 * Set of utilities to work with enums. 
 * 
 * @author marcel
 */
public class EnumUtil {

    /**
     * Takes a list of enum values and returns one of them which was selected by a provided selector (which defines
     * the selection strategy). 
     * 
     * @param <T> - The enum type
     * @param values - An array of enums which are searched 
     * @param selector - Specifies how the values should be searched (defines the used strategy)
     * @return Optional enum from values which was found by a provided selector
     */
    public @Nonnull static <T extends Enum<T>> Optional<T> castFromArray(@Nonnull T[] values, 
            @Nonnull Predicate<T> selector) {
        return castFromArray(values, NullHelpers.notNull(Arrays.asList(selector)));
    }

    /**
     * Filters a provided list by the provided strategie and returns the first enum which matches one of the given 
     * filter strategies. 
     *  
     * @param <T> - The enum type
     * @param values - An array of searched enums of type T
     * @param selectorList - Contains strategies which are used for selecting items item of the values
     * @return Optional enum from values which was found by a provided selector
     */
    @SuppressWarnings("null")
    public @Nonnull static <T extends Enum<T>> Optional<T> castFromArray(@Nonnull T[] values, 
            @Nonnull List<Predicate<T>> selectorList) {
        Optional<T> castedEnum = Optional.empty();
        for (Predicate<T> singleSelector : selectorList) {
            castedEnum = Arrays.stream(values).filter(singleSelector).findFirst();
            if (castedEnum.isPresent()) {
                break;
            }
        }
        return castedEnum;
    }
}
