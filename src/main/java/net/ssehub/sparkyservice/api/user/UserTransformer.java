package net.ssehub.sparkyservice.api.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

public interface UserTransformer {

    /**
     * Converts a given {@link UserDetails} to {@link User}. May perform
     * database operations or some other heavy tasks. The details must be a
     * supported implementation (probably anything which extends from
     * {@link User}).
     * 
     * @param details typically provided by spring security during authentication
     *                process
     * @return StoredUser object with the values from the user details
     * @throws UserNotFoundException Is thrown if too much information are missing
     *                               and the user could not loaded from the data
     *                               storage
     */
    @Nonnull User extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException;

    /**
     * Use the information from the given principal object in order to return a full user object. 
     * If there are not enough information in the application cache, this object may comes directly from the database. 
     * 
     * @param principal
     * @return User which belongs to the given principal
     * @throws UserNotFoundException If the user is not found in this application (database or cache)
     */
    @Nonnull User extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException;

    /**
     * Tries to extend the given object to a user object. Many database operations could be possible. 
     * <br>
     * This operation is only possible if this object is one of the following supported implementations:
     * 
     * <ul><li> {@link SparkysAuthPrincipal}
     * </li>li> {@link UserDetails}
     * </li><li> {@link UserDto}
     * </ul>
     * 
     * @param principal object which is converted or extended to a user object
     * @return extend User  - may be null in case of unsupported principal
     * @throws MissingDataException If the principal object is a supported implementation but does not hold enough 
     *                              information.
     */
    @Nullable User extendFromAny(@Nullable Object principal) throws MissingDataException;

    /**
     * Tries to extend the information of the given data transfer object in order to create a user. 
     * 
     * @param user DTO object which should be casted or extended to a {@link User} object
     * @return User
     * @throws MissingDataException Is thrown if to less information are available for extended the DTO to a user
     */
    @Nonnull User extendFromUserDto(@Nullable UserDto user) throws MissingDataException;

    @Nonnull User extendFromAuthentication(@Nullable Authentication auth) throws MissingDataException;
}
