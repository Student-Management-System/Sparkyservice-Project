package net.ssehub.sparkyservice.api.auth.local;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;

@Component("defaultRealm")
@ParametersAreNonnullByDefault
public class LocalRealm implements UserRealm {

    public static final String IDENTIFIER_NAME = "LOCAL";
    @Nonnull
    private final AuthenticationProvider provider;

    @Autowired
    private LocalRealm(@Lazy @Qualifier("localDbAuthProvider") AuthenticationProvider provider) {
        this.provider = provider;
    }
    
    /**
     * Constructor only for test cases!
     */
    public LocalRealm() {
        this.provider = new AuthenticationProvider() {
            
            @Override
            public boolean supports(@Nullable Class<?> authentication) {
                return false;
            }
            
            @Override
            public Authentication authenticate(@Nullable Authentication authentication) throws AuthenticationException {
                return authentication;
            }
        };
    }

    @Override
    public @Nonnull String identifierName() {
        return IDENTIFIER_NAME;
    }

    @Override
    public @Nonnull String publicName() {
        return "e-learning";
    }

    @Override
    public @Nonnull SparkyUserFactory<? extends SparkyUser> userFactory() {
        return new LocalUserFactory(this);
    }

    @Override
    @Nonnull
    public AuthenticationProvider authenticationProvider() {
        return provider;
    }

    @Override
    public int authenticationPriorityWeight() {
        return 2;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this.nameEquals(obj);
    }
}
