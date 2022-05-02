package net.ssehub.sparkyservice.api.testconf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.SparkyUserFactory;
import net.ssehub.sparkyservice.api.user.UserRealm;

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
    private final SparkyUserFactory<SparkyUser> factory;
    
    public DummyRealm(@Nonnull String identifier) {
        this(identifier, null);
    }
    
    public DummyRealm(@Nonnull String identifier, @Nullable SparkyUserFactory<SparkyUser> factory) {
        this.identifier = identifier;
        this.factory = factory;
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

    @Override
    @Nonnull
    public SparkyUserFactory<? extends SparkyUser> userFactory() {
        var f = factory;
        if (f == null) {
            throw new UnsupportedOperationException("No factory for dummy realm provided");
        }
        return f;
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
