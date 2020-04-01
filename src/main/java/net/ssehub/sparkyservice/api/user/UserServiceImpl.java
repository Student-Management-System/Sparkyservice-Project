package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

/**
 *
 * @author Marcel
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository repository;

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends User> void storeUser(@Nonnull T user) {
        User stUser = new User((User) user);
        if (user.getRealm() == null || user.getUserName().isBlank()) {
            throw new IllegalArgumentException("Realm and username must not be blank."); 
        } else if (user.getRealm() != UserRealm.UNKNOWN) {
            repository.save(stUser);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull List<User> findUsersByUsername(@Nullable String username) throws UserNotFoundException {
        Optional<List<User>> usersByName = repository.findByuserName(username);
        usersByName.orElseThrow(() -> new UserNotFoundException("No user with this name was found in database"));
        usersByName.get().forEach(user -> new LocalUserDetails(notNull(user)));
        var list = usersByName.get();
        List<User> userList = new ArrayList<User>();
        for (User transformUser : list) {
            if (transformUser.getRealm().equals(LocalUserDetails.DEFAULT_REALM)) {
                userList.add(new LocalUserDetails(transformUser));
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
    public @Nonnull User findUserByNameAndRealm(@Nullable String username, @Nullable UserRealm realm) 
            throws UserNotFoundException {
        Optional<User> user = repository.findByuserNameAndRealm(username, realm);
        user.orElseThrow(() -> new UserNotFoundException("no user with this name in the given realm"));
        if (user.get().getRealm() == LocalUserDetails.DEFAULT_REALM) {
            return user.map(LocalUserDetails::new).get();
        } else {
            return user.get();
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull LocalUserDetails findUserById(int id) throws UserNotFoundException {
        Optional<User> user = repository.findById(id);
        user.orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return user.map(LocalUserDetails::new).get();
    }

    /**
     * This method only searches for users with the given name which are in {@link UserRealm#LOCAL}.
     * {@inheritDoc}
     */
    @Override
    public @Nonnull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            if (username == null) {
                throw new UsernameNotFoundException("User with name \"null\" not found");
            } 
            final var storedUser = findUserByNameAndRealm(username, LocalUserDetails.DEFAULT_REALM);
            return new LocalUserDetails(storedUser);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isUserInDatabase(@Nullable User user) {
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

    @Override
    public UserTransformer getDefaultTransformer() {
        return new LightUserTransformerImpl(this);
    }
}
