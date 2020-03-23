package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;

import org.springframework.security.core.AuthenticatedPrincipal;

public interface SparkysAuthPrincipal extends AuthenticatedPrincipal {
    @Nonnull String getName();
    @Nonnull String getRealm();
}
