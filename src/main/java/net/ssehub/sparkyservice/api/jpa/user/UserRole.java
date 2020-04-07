package net.ssehub.sparkyservice.api.jpa.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.GrantedAuthority;

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

    UserRole(@Nonnull String authority) {
        this.authority = authority;
    }

    @Override
    public @Nonnull String getAuthority() {
        return authority;
    }

    public @Nonnull String getRoleValue() {
        return authority;
    }

    /**
     * Casting string to an enum values. 
     * <br> Example Inputs:<br>
     * <code>
     * <ul><li> ADMIN
     * </li><li> ROLE_ADMIN </ul>
     * </code>
     * @param value
     * @return
     */
    public @Nonnull UserRole getEnum(@Nullable String value) throws IllegalArgumentException {
        for (UserRole v : values()) {
            if (v.getRoleValue().equalsIgnoreCase(value)) {
                return v;
            }
        }
        for (UserRole v : values()) {
            if (v.name().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException();
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
