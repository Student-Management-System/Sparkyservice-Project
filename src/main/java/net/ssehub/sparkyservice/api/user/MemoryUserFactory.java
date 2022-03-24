package net.ssehub.sparkyservice.api.user;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides factory methods for {@link MemoryUser}.
 * 
 * @author marcel
 */
public class MemoryUserFactory implements AbstractSparkyUserFactory<MemoryUser> {

    @Override
    @Nonnull
    public MemoryUser create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role,
            boolean isEnabled) {
        if (nickname == null || role == null) {
            throw new IllegalArgumentException("Username and role are mandatory");
        }
        var newUser = new MemoryUser(nickname, password, role);
        newUser.setExpireDate((LocalDate) null);
        return newUser;
    }

    @Override
    @Nonnull
    public MemoryUser create(@Nonnull UserDto userDto) {
        throw new UnsupportedOperationException("Memory user creation not supported via DTO");
    }

    @Override
    @Nonnull
    public MemoryUser create(@Nonnull User jpaUser) {
        throw new UnsupportedOperationException("Memory user creation not supported via JpaUser");
    }

}
