package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.ListUtil;

/**
 * Business logic for user database actions. 
 * 
 * @author Marcel
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository repository;

    private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * Checks if a user is present with the provided search actions with error handling. 
     * 
     * @param user - The user to search for
     * @param userSearchAction - Methods to use for search
     * @return <code>true</code> When the user was found by any provided action
     */
    @SafeVarargs
    public static boolean checkForUser(final User user, final Function<User, User>... userSearchAction) {
        List<Function<User, User>> actionList = Stream.of(userSearchAction).map(Function.identity())
                .collect(Collectors.toList());
        return actionList.stream().dropWhile(searchAction -> {
            boolean found;
            try {
                found = Optional.ofNullable(user).map(searchAction).orElse(null) != null;
            } catch (UserNotFoundException e) {
                found = false;
            }
            return !found; //if not found, drop action and try next one
        }).count() > 0;
    }

    /**
     * Assigns the right type to the given user. 
     * 
     * @param user - User which 
     * @return User with same values but the concrete implementation may has changed
     */
    public @Nonnull static User transformToLocalUser(@Nonnull User user) {
        User returnUser;
        if (user.getRealm().equals(LocalUserDetails.DEFAULT_REALM)) {
            returnUser = new LocalUserDetails(user);
        } else {
            returnUser = user;
        }
        return returnUser;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends User> void storeUser(@Nonnull T user) {
        User stUser = new User((User) user);
        if (user.getRealm() == null || user.getUserName().isBlank()) {
            throw new IllegalArgumentException("Realm and username must not be blank.");
        } else if (user.getRealm() != UserRealm.UNKNOWN && user.getRealm() != UserRealm.MEMORY) {
            log.debug("Try to store user {}@{} into database", stUser.getUserName(), stUser.getRealm());
            repository.save(stUser);
            log.debug("...stored");
        } else {
            log.debug("Dont safe user: {}@{}", stUser.getUserName(), stUser.getRealm());
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull List<User> findUsersByUsername(@Nullable String username) {
        Optional<List<User>> usersByName = repository.findByuserName(username);
        List<User> userList = usersByName.orElseGet(ArrayList::new)
            .stream()
            .map(UserServiceImpl::transformToLocalUser)
            .collect(Collectors.toList());
        return notNull(userList);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull User findUserByNameAndRealm(@Nullable String username, @Nullable UserRealm realm) 
            throws UserNotFoundException {
        Optional<User> optUser = repository.findByuserNameAndRealm(username, realm);
        User user =  optUser.map(UserServiceImpl::transformToLocalUser).orElseThrow(
            () -> new UserNotFoundException("no user with this name in the given realm"));
        return notNull(user);
    } 

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull LocalUserDetails findUserById(int id) throws UserNotFoundException {
        Optional<User> user = repository.findById(id);
        var localUser = user.map(LocalUserDetails::new)
                .orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return notNull(localUser);
    }

    /**
     * This method only searches for users with the given name which are in {@link UserRealm#LOCAL}.
     * {@inheritDoc}
     */
    @Override
    public @Nonnull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {            
            var userDetails = Optional.ofNullable(username)
                .map(name -> findUserByNameAndRealm(name, LocalUserDetails.DEFAULT_REALM))
                .map(LocalUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User with name \"null\" not found"));
            return notNull(userDetails);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isUserInDatabase(@Nullable User user) {
        return checkForUser(user, 
            u -> this.findUserById(u.getId()), 
            u -> this.findUserByNameAndRealm(u.getUserName(), u.getRealm())
         );
    }

    @Override
    public @Nonnull List<User> findAllUsers() {
        Iterable<User> optList = repository.findAll();
        return ListUtil.toList(optList);
    }

    @Override
    public void deleteUser(@Nullable User user) {
        Optional.ofNullable(user).ifPresentOrElse(repository::delete, () -> log.info("Can't delete null user"));
    }

    @Override
    public void deleteUser(@Nullable String username, @Nullable UserRealm realm) {
        User user;
        try {
            user = findUserByNameAndRealm(username, realm);
            deleteUser(user);
        } catch (UserNotFoundException e) {
            log.info("Can't delete user with name: " + username + "|" + realm + " it was not found in database");
        }
    }

    @Override
    public List<User> findAllUsersInRealm(@Nullable UserRealm realm) {
        Iterable<User> optList = repository.findByRealm(realm);
        return ListUtil.toList(optList);
    }
}
