package net.ssehub.sparkyservice.api.storeduser;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.db.user.StoredUser;

/**
 *
 * @author Marcel
 */
@Service
public class StoredUserService implements IStoredUserService {

    @Autowired
    private StoredUserRepository repository;

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends StoredUser> void storeUser(@Nonnull T user) {
        StoredUser stUser = new StoredUser((StoredUser) user);
        repository.save(stUser);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull List<StoredUser> findUsersByUsername(@Nullable String username) throws UserNotFoundException {
        Optional<List<StoredUser>> usersByName = repository.findByuserName(username);
        usersByName.orElseThrow(() -> new UserNotFoundException("No user with this name was found in database"));
        usersByName.get().forEach(x -> new StoredUserDetails(x));
        var list = usersByName.get();
        List<StoredUser> userList = new ArrayList<StoredUser>();
        for (StoredUser transformUser : list) {
            if (transformUser.getRealm().equals(StoredUserDetails.DEFAULT_REALM)) {
                userList.add(new StoredUserDetails(transformUser));
            } else {
                userList.add(transformUser);
            }
        }
        return userList;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull StoredUser findUserByNameAndRealm(@Nullable String username, @Nullable String realm) 
            throws UserNotFoundException {
        Optional<StoredUser> user = repository.findByuserNameAndRealm(username, realm);
        user.orElseThrow(() -> new UserNotFoundException("no user with this name in the given realm"));
        if (user.get().getRealm().equals(StoredUserDetails.DEFAULT_REALM)) {
            return user.map(StoredUserDetails::new).get();
        } else {
            return user.get();
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull StoredUserDetails findUserById(int id) throws UserNotFoundException {
        Optional<StoredUser> user = repository.findById(id);
        user.orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return user.map(StoredUserDetails::new).get();
    }

    /**
     * This method only searched in the {@link StoredUserDetails#DEFAULT_REALM} for usernames.
     * {@inheritDoc}
     */
    @Override
    public @Nonnull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            if (username == null) {
                throw new UsernameNotFoundException("User with name \"null\" not found");
            } 
            final var storedUser = findUserByNameAndRealm(username, StoredUserDetails.DEFAULT_REALM);
            return new StoredUserDetails(storedUser);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isUserInDatabase(@Nullable StoredUser user) {
        if (user != null) {
            try {
                this.findUserById(user.getId());
                return true;
            } catch (UserNotFoundException e) {
                try {
                    this.findUserByNameAndRealm(user.getUserName(), user.getRealm());
                    return true;
                } catch (UserNotFoundException ex) {
                    
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}.
     */
    public @Nonnull StoredUser convertUserDetailsToStoredUser(@Nullable UserDetails details) {
        var optDetails = Optional.ofNullable(details);
        if (optDetails.isPresent() && optDetails.get().getUsername() != null) {
            @Nullable StoredUser storeUser = castUserToStoredOne(notNull(optDetails.get()));
            if (storeUser != null && !isUserInDatabase(storeUser)) {
                return storeUser;
            }
        }
        throw new UnsupportedOperationException("Converting with the given user details not supported.");
    }

    /**
     * Cast user details to stored user and tries to use the explicit implementation. 
     * <br>
     * Supported implementations: <br>
     * <ul><li> {@link StoredUserDetails}</li>
     * <li>{@link LdapUserDetailsImpl}</li></ul>
     * 
     * @param details typically provided by spring security during authentication process
     * @return StoredUser which holds data from the given details
     */
    private @Nullable StoredUser castUserToStoredOne(@Nonnull UserDetails details) {
        StoredUser storeUser = null;
        if (details instanceof LdapUserDetailsImpl) {
            storeUser = new StoredUser(
                    notNull(details.getUsername()),
                    null, 
                    "LDAP", 
                    details.isEnabled(),
                    notNull(UserRole.DEFAULT.name()));
        } else if (details instanceof StoredUserDetails || details instanceof StoredUser)  {
            storeUser = (StoredUser) details;
        }
        return storeUser;
    }
}
