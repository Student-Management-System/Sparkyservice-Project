package net.ssehub.sparkyservice.api.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides an definition of for user factories. AbstractFactory design pattern.
 * 
 * @author marcel
 *
 * @param <T> Desired User implementation which the factory is
 */
@ParametersAreNonnullByDefault
public interface AbstractSparkyUserFactory<T extends SparkyUser> {

    /**
     * Creates a new user with the minimum set of needed values.
     * 
     * @param nickname Name of the user which must be unique per realm
     * @param password
     * @param role
     * @param isEnabled
     * @return {@link SparkyUser} implementation
     */
    @Nonnull
    T create(@Nullable String nickname, @Nullable Password password, @Nullable UserRole role, boolean isEnabled);
    
    /**
     * Creates a {@link SparkyUser} via userDto values.
     * 
     * @param userDto
     * @return {@link SparkyUser} implementation with values from the given DTO
     */
    @Nonnull
    T create(UserDto userDto);
    
    /**
     * Creates a new user with values from the given jpa implementation. This method can be used to create 
     * user from database values which means this method can be used for transformation of database values to user 
     * objects.
     * 
     * @param jpaUser
     * @return {@link SparkyUser} implementation with storage values
     */
    @Nonnull
    T create(User jpaUser);
}
