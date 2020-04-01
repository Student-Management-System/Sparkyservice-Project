package net.ssehub.sparkyservice.api.user;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

/**
 * Business logic for {@link User} and {@link LocalUserDetails}. This class is also used for a 
 * {@link UserDetailsService} authentication method through spring. It must provide a method for 
 * 
 * @author Marcel
 */
public interface IUserService extends UserDetailsService {

    /**
     * Make a user persistent (in any kind of data storage).
     * 
     * @param <T> A class which must extends from {@link User} (which holds the JPA definitions).
     * @param user Is saved in a persistence way (must hold username and realm)
     */
    <T extends User> void storeUser(@Nonnull T user);

    /**
     * Searches a data storage for an explicit user identified by unique id. 
     * 
     * @param id unique identifier
     * @return StoredUser with the id
     * @throws UserNotFoundException If no user with the given id is found in data storage
     */
    @Nonnull User findUserById(int id) throws UserNotFoundException;

    /**
     * Searches a data storage for all users with a given username. 
     * 
     * @param username 
     * @return
     * @throws UserNotFoundException
     */
    @Nonnull List<User> findUsersByUsername(@Nullable String username) throws UserNotFoundException;

    
    @Nonnull User findUserByNameAndRealm(@Nullable String username, @Nullable String realm) throws UserNotFoundException;

    /**
     * Checks if the given user is already stored in the used data storage. This could used as an indicator if the 
     * user will be edited or a new one is created.
     * 
     * @param user The user to check
     * @return <code>true</code> if the user was already stored in the past, <code>false</code> otherwise
     */
    boolean isUserInDatabase(@Nullable User user);

    /**
     * Is used by SpringSecurity for getting user details with a given username. It returns a single UserDetails
     * without limiting the search to a specific realm. Through this, a specific realm is always preferred (typically
     * the realm which is used for local authentication).
     * 
     * @param username name to look for
     * @return userDetails Details loaded from a data storage which is identified by the given username
     * @throws When a the given username is not found in storage (spring will continue 
     * with the next configured {@link AuthenticationProvider})
     */
    @Override
    @Nonnull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * The default transformer used by this service.
     * @return 
     */
    UserTransformer getDefaultTransformer();
}
