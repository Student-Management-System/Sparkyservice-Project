package net.ssehub.sparkyservice.api.user.storage;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.access.annotation.Secured;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

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
     * Creates a new entry in the storage for the given user.
     * 
     * @param username
     * @return The created and saved user
     */
    @Secured(UserRole.FullName.ADMIN)
    @Nonnull
    LocalUserDetails addUser(@Nonnull String username);

    /**
     * Searches a data storage for an explicit user identified by unique id.
     * 
     * @param id
     *           unique identifier
     * @return StoredUser with the id
     * @throws UserNotFoundException
     *                               If no user with the given id is found in data storage
     */
    @Nonnull
    SparkyUser findUserById(int id) throws UserNotFoundException;

    /**
     * Searches a data storage for all users with a given username.
     * 
     * @param username
     * @return A list of users which shares the same username in different realms
     * @throws UserNotFoundException
     */
    @Nonnull
    List<SparkyUser> findUsersByUsername(@Nullable String username) throws UserNotFoundException;

    /**
     * Finds a specific user by name and realm.
     * 
     * @param username
     * @param realm
     * @return Specific user
     * @throws UserNotFoundException
     */
    @Nonnull
    SparkyUser findUserByNameAndRealm(@Nullable String username, @Nullable UserRealm realm) 
        throws UserNotFoundException;

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
     * Deletes the given user.
     * 
     * @param user
     */
    void deleteUser(SparkyUser user);

    /**
     * Deletes a user specific user identified by his realm and username.
     * 
     * @param username
     * @param realm
     */
    @Secured(UserRole.FullName.ADMIN)
    void deleteUser(String username, UserRealm realm);

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
        return this.findUserByNameAndRealm(user.getUsername(), user.getRealm());
    }
}
