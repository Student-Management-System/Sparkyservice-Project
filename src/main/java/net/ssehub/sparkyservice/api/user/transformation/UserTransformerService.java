package net.ssehub.sparkyservice.api.user.transformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;

public interface UserTransformerService {

    /**
     * Converts a given {@link UserDetails} to {@link User}. May perform
     * storage operations or some other heavy tasks. The details must be a
     * supported implementation (probably anything which extends from
     * {@link User}).
     * 
     * @param details typically provided by spring security during authentication
     *                process
     * @return SparkyUser object with the values from the UserDetails
     * @throws UserNotFoundException Is thrown if too much information are missing
     *                               and the user could not loaded from the data
     *                               storage
     */
    @Nonnull SparkyUser extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException;

    /**
     * Use the information from the given principal object in order to return a full user object. 
     * If there are not enough information in the application cache, this object may comes directly from a stirage. 
     * 
     * @param principal
     * @return SparkyUser which belongs to the given principal
     * @throws UserNotFoundException If the user is not found in this application (database or cache)
     */
    @Nonnull SparkyUser extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException;

    /**
     * Tries to extend the given principal object to a user object. Many database operations could be possible. 
     * <br>
     * Principals in general can contain an instance of {@link SparkysAuthPrincipal} or a {@link UserDetails}. 
     * 
     * @param principal object which is converted or extended to a user object - typically provided by springs
     *                  {@link Authentication#getPrincipal()}
     * @return extend SparkyUser  - may be null in case of unsupported principal
     * @throws MissingDataException If the principal object is a supported implementation but does not hold enough 
     *                              information.
     */
    @Nullable SparkyUser extendFromAnyPrincipal(@Nullable Object principal) throws MissingDataException;

    /**
     * Tries to extend the information of the given data transfer object in order to create a user. 
     * 
     * @param user DTO object which should be casted or extended to a {@link User} object
     * @return User
     * @throws MissingDataException Is thrown if to less information are available for extended the DTO to a user
     */
    @Nonnull SparkyUser extendFromUserDto(@Nullable UserDto user) throws MissingDataException;

    /**
     * Tries to extract information from an authentication object and create an usable user of it. 
     * There is no guarantee that the information match with those from a storage. 
     * 
     * @param auth
     * @return
     * @throws MissingDataException
     */
    @Nonnull SparkyUser extendFromAuthentication(@Nullable Authentication auth) throws MissingDataException;
}
