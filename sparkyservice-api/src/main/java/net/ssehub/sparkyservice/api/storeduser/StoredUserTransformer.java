package net.ssehub.sparkyservice.api.storeduser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.userdetails.UserDetails;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.db.user.StoredUser;

public interface StoredUserTransformer {

    /**
     * Converts a given {@link UserDetails} to {@link StoredUser}. May perform
     * database operations or some other heavy tasks. The details must be a
     * supported implementation (probably anything which extends from
     * {@link StoredUser}).
     * 
     * @param details typically provided by spring security during authentication
     *                process
     * @return StoredUser object with the values from the user details
     * @throws UserNotFoundException Is thrown if too much information are missing
     *                               and the user could not loaded from the data
     *                               storage
     */
    @Nonnull StoredUser extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException;

    /**
     * Tries to cast a given instance of {@link UserDetails} to a {@link StoredUser}
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
    @Nonnull StoredUser castFromUserDetails(@Nullable UserDetails details) throws MissingDataException;

    @Nonnull StoredUser extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException;
}
