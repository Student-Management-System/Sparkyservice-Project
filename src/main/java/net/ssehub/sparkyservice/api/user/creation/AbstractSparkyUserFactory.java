package net.ssehub.sparkyservice.api.user.creation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

@ParametersAreNonnullByDefault
public interface AbstractSparkyUserFactory<T extends SparkyUser> {

    @Nonnull
    T create(@Nullable String username, @Nullable Password password, @Nullable UserRole role, boolean isEnabled);

    @Nonnull
    T create(UserDto userDto);
    
    @Nonnull
    T create(User jpaUser);
}
