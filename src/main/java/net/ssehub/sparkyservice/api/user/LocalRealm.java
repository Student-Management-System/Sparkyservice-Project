package net.ssehub.sparkyservice.api.user;

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

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

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

    // public for test cases
    public static class LocalUserFactory implements SparkyUserFactory<LocalUserDetails> {
        @Nonnull
        private final UserRealm associatedRealm;

        // public visibility for testing
        public LocalUserFactory(UserRealm associatedRealm) {
            super();
            this.associatedRealm = associatedRealm;
        }

        @Override
        @Nonnull
        public LocalUserDetails create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role,
            boolean isEnabled) {
            if (nickname == null || role == null) {
                throw new IllegalArgumentException("Username and role are mandatory");
            }
            var newUser = new LocalUserDetails(nickname, associatedRealm, password, isEnabled, role);
            newUser.setExpireDate(LocalDate.now().plusMonths(6));
            return newUser;
        }

        @Override
        @Nonnull
        public LocalUserDetails create(@Nonnull UserDto userDto) {
            final var pwDto = userDto.passwordDto;
            final String username = userDto.username;
            final var role = userDto.role;
            if (username == null || role == null || pwDto == null) {
                throw new IllegalArgumentException("Username, role and password are mandatory");
            }
            return new LocalUserDetails(userDto);
        }

        @Override
        @Nonnull
        public LocalUserDetails create(@Nonnull User jpaUser) {
            return new LocalUserDetails(jpaUser);
        }
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
