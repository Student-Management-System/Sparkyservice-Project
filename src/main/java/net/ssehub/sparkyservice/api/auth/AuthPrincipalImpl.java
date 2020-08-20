package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;

/**
 * Implementation of {@link SparkysAuthPrincipal}.
 * 
 * @author marcel
 */
class AuthPrincipalImpl implements SparkysAuthPrincipal {
    private @Nonnull UserRealm realm;

    private @Nonnull String name;
    
    /**
     * Holds a name and a Relam. Can be used for unique identification of a user.
     * 
     * @param realmString - {@link UserRealm} as string
     * @param name - The username of a user 
     */
    public AuthPrincipalImpl(@Nullable String realmString, @Nullable String name) {
        if (realmString == null || name == null) {
            throw new IllegalArgumentException("Arguments must have a value");
        } 
        try {
            this.realm = UserRealm.valueOf(realmString);
        } catch (IllegalArgumentException e) {
            this.realm = UserRealm.UNKNOWN;
        }
        this.name = name;
    }

    @Override
    public @Nonnull UserRealm getRealm() {
        return realm;
    }

    @Override
    public @Nonnull String getName() {
        return name;
    }

}
