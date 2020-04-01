package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;

import org.springframework.security.core.AuthenticatedPrincipal;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;

public interface SparkysAuthPrincipal extends AuthenticatedPrincipal {
    @Nonnull String getName();
    @Nonnull UserRealm getRealm();
}
