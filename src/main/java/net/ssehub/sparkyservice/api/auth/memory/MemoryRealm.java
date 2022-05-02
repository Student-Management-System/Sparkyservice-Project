package net.ssehub.sparkyservice.api.auth.memory;

import java.time.LocalDate;

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
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

@Component
@ParametersAreNonnullByDefault
public class MemoryRealm implements UserRealm, SparkyUserFactory<MemoryUser> {

    public static final String IDENTIFIER_NAME = "MEMORY";

    @Nonnull
    private final AuthenticationProvider provider;

    @Autowired
    private MemoryRealm(@Lazy @Qualifier("memoryAuthProvider") AuthenticationProvider provider) {
        this.provider = provider;
    }

    /**
     * Constructor only for test cases!
     */
    public MemoryRealm() {
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
        return "RECOVERY";
    }

    @Override
    public @Nonnull SparkyUserFactory<? extends SparkyUser> userFactory() {
        return this;
    }

    public @Nonnull AuthenticationProvider authenticationProvider() {
        return provider;
    }

    public int authenticationPriorityWeight() {
        return Integer.MAX_VALUE;
    }
    
    @Override
    @Nonnull
    public MemoryUser create(User jpaUser) {
        throw new UnsupportedOperationException("Memory user creation not supported via JpaUser");
    }

    @Override
    @Nonnull
    public MemoryUser create(UserDto userDto) {
        throw new UnsupportedOperationException("Memory user creation not supported via DTO");
    }

    @Override
    @Nonnull
    public MemoryUser create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role,
        boolean isEnabled) {
        if (nickname == null || role == null) {
            throw new IllegalArgumentException("Username and role are mandatory");
        }
        var newUser = new MemoryUser(nickname, this, password, role);
        newUser.setExpireDate((LocalDate) null);
        return newUser;
    }
}
