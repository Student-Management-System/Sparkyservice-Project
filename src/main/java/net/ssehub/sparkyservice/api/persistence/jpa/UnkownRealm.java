package net.ssehub.sparkyservice.api.persistence.jpa;

import javax.annotation.Nonnull;

import org.springframework.security.authentication.AuthenticationProvider;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;

/**
 * Unkown realm which specifies nothing. Should only be used for methods where a Realm is needed but is unkown at that 
 * moment. Make sure the correct realm is set afterwards. An example case is hibernate or test cases, where
 * default constructors are necessary.
 */
public class UnkownRealm implements UserRealm {

    @Override
    public @Nonnull String identifierName() {
        return "UNKOWN";
    }

    @Override
    public @Nonnull String publicName() {
        return "UNKOWN";
    }

    @Override
    public @Nonnull SparkyUserFactory<? extends SparkyUser> userFactory() {
        throw new UnsupportedOperationException("UNKOWN Realm");
    }

    @Override
    public @Nonnull AuthenticationProvider authenticationProvider() {
        throw new UnsupportedOperationException("Unkown realm has no authentication provider");
    }

    @Override
    public int authenticationPriorityWeight() {
        return Integer.MIN_VALUE;
    }
    
}