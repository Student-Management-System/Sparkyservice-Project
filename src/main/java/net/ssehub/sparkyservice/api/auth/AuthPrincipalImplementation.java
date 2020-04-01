package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class AuthPrincipalImplementation implements SparkysAuthPrincipal {
    private @Nonnull String realm;

    private @Nonnull String name;

    public AuthPrincipalImplementation(@Nullable String realm, @Nullable String name) {
        super();
        if (realm == null || name == null) {
            throw new IllegalArgumentException("Arguments must have a value");
        }
        this.realm = realm;
        this.name = name;
    }

    @Override
    public @Nonnull String getRealm() {
        return realm;
    }

    @Override
    public @Nonnull String getName() {
        return name;
    }

}
