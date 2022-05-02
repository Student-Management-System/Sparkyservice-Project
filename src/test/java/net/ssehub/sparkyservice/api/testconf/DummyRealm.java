package net.ssehub.sparkyservice.api.testconf;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;

public class DummyRealm implements UserRealm {
    
    @Nonnull
    private static final AuthenticationProvider DUMMY_PROVIDER = new AuthenticationProvider() {
        
        @Override
        public boolean supports(Class<?> authentication) {
            return false;
        }
        
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            throw new BadCredentialsException("No supported");
        }
    };

    @Nonnull
    private final String identifier;
    
    @Nullable
    private final Function<UserRealm, SparkyUserFactory<?>> factorySupply;
    
    public DummyRealm(@Nonnull String identifier) {
        this(identifier, null);
    }
    
    public DummyRealm(@Nonnull String identifier, @Nullable  Function<UserRealm, SparkyUserFactory<?>> factory) {
        this.identifier = identifier;
        this.factorySupply = factory;
    }

    @Override
    @Nonnull
    public String identifierName() {
        return identifier;
    }

    @Override
    @Nonnull
    public String publicName() {
        return identifierName();
    }

    @SuppressWarnings("null")
    @Override
    @Nonnull
    public SparkyUserFactory<? extends SparkyUser> userFactory() {
        var function = factorySupply;
        if (function == null) {
            throw new UnsupportedOperationException("No factory for this dummy realm");
        }
        return function.apply(this);
    }

    @Override
    @Nonnull
    public AuthenticationProvider authenticationProvider() {
        return DUMMY_PROVIDER;
    }

    @Override
    public int authenticationPriorityWeight() {
        return 0;
    }

}
