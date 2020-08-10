package net.ssehub.sparkyservice.api.jpa.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.*;

import java.util.ArrayList;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.GrantedAuthority;

import net.ssehub.sparkyservice.api.util.EnumUtil;
import net.ssehub.sparkyservice.api.util.NonNullByDefault;

/**
 * Defines the (permission) role of a given user.
 *  
 * @author marcel
 */
@NonNullByDefault
public enum UserRole implements GrantedAuthority {
    
    /**
     * Default user group. All authenticated user will probably hold this role if they have no other. 
     */
    DEFAULT(FullName.DEFAULT), 

    /**
     * Full permission to all services provided by this project.
     */
    ADMIN(FullName.ADMIN), 
    
    /**
     * Permission to access all routed paths in order to reach protected micro services. 
     */
    SERVICE(FullName.SERVICE);

    private @Nonnull final String authority;

    /**
     * Initialize the authority value while creating a new enum instance. This authority can be used for creating 
     * enums which are identified by the provided string later via {@link #getEnum(String)}.
     * 
     * @param authority
     */
    UserRole(@Nonnull String authority) {
        this.authority = authority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthority() {
        return authority;
    }

    /**
     * A name of the enum which is valid to cast it.
     * 
     * @return name of the enum as string
     */
    public String getRoleValue() {
        return authority;
    }

    /**
     * Returns an enum which can be identified by the provieded string. 
     * Non case sensitive. Any of {@link FullName} are valid.
     * <br> Example Inputs:<br>
     * <code>
     * <ul><li> ADMIN
     * </li><li> ROLE_ADMIN </ul>
     * </code><br>
     * @param value - A string which probably identifies
     * @return the string as enumCan't get an instance of UserRole of this string.
     */
    public @Nonnull UserRole getEnum(@Nullable String value) {
        var strategyList = new ArrayList<Predicate<UserRole>>();
        strategyList.add(e -> e.getRoleValue().equalsIgnoreCase(value));
        strategyList.add(e -> e.name().equalsIgnoreCase(value));
        
        var returnValue = EnumUtil.<UserRole>castFromArray(values(), strategyList).orElseThrow(
            () -> new IllegalArgumentException("Can't get an instance of UserRole of this string."));
        return notNull(returnValue);
    } 

    /**
     * Use this for getting the ROLE_ prefix of the enum value.
     *
     * @author Marcel
     */
    public class FullName {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String DEFAULT = "ROLE_DEFAULT";
        public static final String SERVICE = "ROLE_SERVICE";
    }
}
