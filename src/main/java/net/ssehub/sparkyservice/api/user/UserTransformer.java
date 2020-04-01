package net.ssehub.sparkyservice.api.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
     * Tries to cast a given instance of {@link UserDetails} to a {@link User}
     * object. This is done without database interactions and is more resource
     * friendly than {@link #extendFromUserDetails(UserDetails)} but may can't
     * complete the challenge if some really essential information are missing
     * (typically identifier).
     * 
     * @param details holds user information
     * @return user object which may be incomplete but never null
     * @throws MissingDataException If to less information are provided to create a
     *                              StoredUser object
     */
    @Nonnull User castFromUserDetails(@Nullable UserDetails details) throws MissingDataException;

    @Nonnull User extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException;

    @Nullable User extendFromAny(@Nullable Object principal) throws MissingDataException, UserNotFoundException;

    @Nonnull User extendFromUserDto(@Nullable UserDto user) throws MissingDataException, UserNotFoundException;
}
