package net.ssehub.sparkyservice.api.persistence;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.access.annotation.Secured;

import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;

/**
 * Business search logic for storage management. Maps {@link User} to {@link SparkyUser}. 
 * 
 * @author Marcel
 */
public interface UserStorageService {

    /**
     * Safes the given user to a persistent storage. When the entry already exists it changes the values.
     * 
     * @param <T>
     *             A class which must extends from {@link User} (which holds the JPA definitions).
     * @param user
     *             Is saved in a persistence way (must hold username and realm)
     */
    <T extends SparkyUser> void commit(@Nonnull T user);

    /**
     * Searches a data storage for all users with a given username.
     * 
     * @param nickname Username without realm information
     * @return A list of users which shares the same nickname in different realms
     * @throws UserNotFoundException
     */
    @Nonnull
    List<SparkyUser> findUsers(@Nullable String nickname) throws UserNotFoundException;

    @Nonnull
    SparkyUser findUser(@Nullable String identName) throws UserNotFoundException;
    
    @Nonnull
    SparkyUser findUser(@Nullable Identity ident) throws UserNotFoundException;
    
    /**
     * A list with all users in the data storage. Never null but may be empty.
     * 
     * @return All storage users
     */
    @Secured(UserRole.FullName.ADMIN)
    @Nonnull
    List<SparkyUser> findAllUsers();

    /**
     * Checks if the given user is already stored in the used data storage. This could used as an indicator if the user
     * will be edited or a new one is created.
     * 
     * @param user
     *             The user to check
     * @return <code>true</code> if the user was already stored in the past, <code>false</code> otherwise
     */
    boolean isUserInStorage(@Nullable SparkyUser user);

    /**
     * Deletes a user specific user identified by his realm and username.
     * 
     * @param username
     * @param realm
     */
    @Secured(UserRole.FullName.ADMIN)
    void deleteUser(Identity user);

    /**
     * Find all users in a given realm (without pagination). Only admins are allowed to do this.
     * 
     * @param realm
     * @return List of users in the given realm
     */
    @Secured(UserRole.FullName.ADMIN)
    List<SparkyUser> findAllUsersInRealm(UserRealm realm);

    /**
     * Load the same user from a storage in order to refresh the values. A new user object is created. 
     * 
     * @param user
     * @return New object with values from a storage
     * @throws UserNotFoundException
     */
    @Nonnull
    default SparkyUser refresh(SparkyUser user) throws UserNotFoundException {
        return this.findUser(user.getIdentity());
    }
}
