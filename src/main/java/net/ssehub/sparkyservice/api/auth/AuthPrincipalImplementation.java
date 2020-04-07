package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;

class AuthPrincipalImplementation implements SparkysAuthPrincipal {
    private @Nonnull UserRealm realm;

    private @Nonnull String name;
    
    public AuthPrincipalImplementation(@Nullable String realmString, @Nullable String name) {
        if (realmString == null || name == null) {
            throw new IllegalArgumentException("Arguments must have a value");
        } 
        try {
            this.realm = UserRealm.valueOf(realmString);
        } catch (Exception e) {
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
