package net.ssehub.sparkyservice.api.auth.local;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

// public for test cases
@ParametersAreNonnullByDefault
public class LocalUserFactory implements SparkyUserFactory<LocalUserDetails> {
    @Nonnull
    private final UserRealm associatedRealm;

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
