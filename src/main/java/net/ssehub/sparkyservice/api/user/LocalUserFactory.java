package net.ssehub.sparkyservice.api.user;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.creation.AbstractSparkyUserFactory;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

public class LocalUserFactory implements AbstractSparkyUserFactory<LocalUserDetails> {

    @Nonnull
    public LocalUserDetails create(@Nullable String username, @Nullable Password password, @Nullable UserRole role,
            boolean isEnabled) {
        if (username == null || role == null) {
            throw new IllegalArgumentException("Username and role are mandatory");
        }
        var newUser = new LocalUserDetails(username, password, isEnabled, role);
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
