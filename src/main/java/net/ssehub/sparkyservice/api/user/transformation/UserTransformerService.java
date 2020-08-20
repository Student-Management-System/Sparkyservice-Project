package net.ssehub.sparkyservice.api.user.transformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.creation.AbstractSparkyUserFactory;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;

/**
 * User information transformation. Extends := storage operation!
 * 
 * @author marcel
 */
public interface UserTransformerService {

    /**
     * Converts a given {@link UserDetails} to {@link User}. May perform
     * storage operations or some other heavy tasks. The details must be a
     * supported implementation: 
     * <ul><li> anything which extends from {@link SparkyUser}
     * </li><li> {@link org.springframework.security.core.userdetails.User}
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
     * If there are not enough information in the application cache, this object may comes directly from a storage. 
     * 
     * @param principal
     * @return SparkyUser which belongs to the given principal
     * @throws UserNotFoundException If the user is not found in this application (database or cache)
     */
    @Nonnull SparkyUser extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException;

    /**
     * Tries to extend the information from authentication to create a user object. 
     * Many database operations could be possible in order to get those information. 
     * <br>
     * Principals in general can contain an instance of {@link SparkysAuthPrincipal} or a {@link UserDetails}. 
     * 
     * @param auth
     * @return 
     * @throws MissingDataException If the principal of the auth is a supported implementation but does not hold enough 
     *                              information.
     */
    @Nonnull SparkyUser extendFromAuthentication(@Nullable Authentication auth);

    /**
     * Tries to extend the information of the given data transfer object in order to create a user. 
     * 
     * @param user DTO object which should be casted or extended to a {@link User} object
     * @return User with information from a storage
     * @throws MissingDataException Is thrown if to less information are available for extended the DTO to a user
     * @see AbstractSparkyUserFactory#create(UserDto) Factory Method with user DTO
     */
    @Nonnull SparkyUser extendFromUserDto(@Nullable UserDto user);

    /**
     * Tries to extract information from an authentication object and create an usable {@link SparkyUser} of it. 
     * There is no guarantee that the information match with those from a storage since here shouldn't be done
     * any storage operations. 
     * 
     * @param auth
     * @return
     * @throws MissingDataException
     */
    @Nonnull SparkyUser extractFromAuthentication(@Nullable Authentication auth);
}
