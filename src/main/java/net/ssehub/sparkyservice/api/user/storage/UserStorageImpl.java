package net.ssehub.sparkyservice.api.user.storage;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.creation.UserFactoryProvider;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Business logic for user database actions. 
 * 
 * @author Marcel
 */
@Service
public class UserStorageImpl implements UserStorageService {

    @Autowired
    private UserRepository repository;

    private final Logger log = LoggerFactory.getLogger(UserStorageImpl.class);

    /**
     * Checks if a user is present with the provided search actions with error handling. 
     * 
     * @param user - The user to search for
     * @param userSearchAction - Methods to use for search
     * @return <code>true</code> When the user was found by any provided action
     */
    @SafeVarargs
    public static boolean checkForUser(SparkyUser user, Function<SparkyUser, SparkyUser>... userSearchAction) {
        var actionList = Stream.of(userSearchAction)
                .map(Function.identity())
                .collect(Collectors.toList());
        return actionList
            .stream()
            .dropWhile(searchAction -> {
                boolean found;
                try {
                    found = Optional.ofNullable(user).map(searchAction).orElse(null) != null;
                } catch (UserNotFoundException e) {
                    found = false;
                }
                return !found; //if not found, drop action and try next one
            })
            .count() > 0;
    }

    /**
     * Creates the correct implementation of the provided user based on the realm.
     * 
     * @param user
     * @return SparkyUser representation of given JPA object
     */
    public @Nonnull static SparkyUser transformUser(@Nonnull User user) {
//        SparkyUser returnUser;
//        switch(user.getRealm()) {
//        case LOCAL:
//            returnUser = new LocalUserDetails(user);
//            break;
//        case LDAP:
//            returnUser = new LdapUser(user);
//        default:
//            throw new UnsupportedOperationException();
//        }
//        return returnUser;
        return UserFactoryProvider.getFactory(user.getRealm()).create(user);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends SparkyUser> void commit(@Nonnull T user) {
        User jpa = user.getJpa();
        if (jpa.getRealm() == null || jpa.getUserName().isBlank()) {
            throw new IllegalArgumentException("Realm and username must not be blank.");
        } else if (user.getRealm() != UserRealm.MEMORY) {
            log.debug("Try to store user {}@{} into database", jpa.getUserName(), jpa.getRealm());
            repository.save(jpa);
            log.debug("...stored");
        } else {
            log.debug("Don't safe user: {}@{}", jpa.getUserName(), jpa.getRealm());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nonnull LocalUserDetails addUser(@Nonnull String username) {
        final var newUser = LocalUserDetails.newLocalUser(username, "", UserRole.DEFAULT);
        if (isUserInStorage(newUser) ) {
            throw new DuplicateEntryException(newUser);
        }
        commit(newUser);
        return newUser;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull List<SparkyUser> findUsersByUsername(@Nullable String username) {
        Optional<List<User>> usersByName = repository.findByuserName(username);
        return notNull(
             usersByName.orElseGet(ArrayList::new)
                .stream()
                .map(UserStorageImpl::transformUser)
                .collect(Collectors.toList())
        );
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull SparkyUser findUserByNameAndRealm(@Nullable String username, @Nullable UserRealm realm) 
            throws UserNotFoundException {
        Optional<User> optUser = repository.findByuserNameAndRealm(username, realm);
        SparkyUser user =  optUser.map(UserStorageImpl::transformUser).orElseThrow(
            () -> new UserNotFoundException("no user with this name in the given realm"));
        return notNull(user);
    } 

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull SparkyUser findUserById(int id) throws UserNotFoundException {
        Optional<User> user = repository.findById(id);
        var localUser = user.map(UserStorageImpl::transformUser)
                .orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return notNull(localUser);
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isUserInStorage(@Nullable SparkyUser user) {
        boolean found = false;
        try {
            
            if (user != null) {
                found = checkForUser(user,
                        u -> this.findUserById(u.getJpa().getId()), 
                        u -> this.findUserByNameAndRealm(u.getUsername(), u.getRealm())
                        );
            } 
        } catch (IllegalArgumentException e) {
        }
        return found;
    }

    @Override
    public @Nonnull List<SparkyUser> findAllUsers() {
        return fromIterableToUserList(repository.findAll());
    }

    @Override
    public void deleteUser(@Nullable SparkyUser user) {
        Optional.ofNullable(user)
            .map(u -> u.getJpa())
            .ifPresentOrElse(repository::delete, () -> log.info("Can't delete null user"));
    }

    @Override
    public void deleteUser(@Nullable String username, @Nullable UserRealm realm) {
        SparkyUser user;
        try {
            user = findUserByNameAndRealm(username, realm);
            deleteUser(user);
        } catch (UserNotFoundException e) {
            log.info("Can't delete user with name: " + username + "|" + realm + " it was not found in database");
        }
    }

    @Override
    public List<SparkyUser> findAllUsersInRealm(@Nullable UserRealm realm) {
        return fromIterableToUserList(repository.findByRealm(realm));
    }

    /**
     * Mapps an iterable list of jpa users to a List of SparkyUsers.
     * 
     * @param list
     * @return List of SparkyUser with the correct implementation type
     */
    public static @Nonnull List<SparkyUser> fromIterableToUserList(Iterable<User> list) {
        if (list == null) {
            @SuppressWarnings("unchecked") Iterable<User> emptyList = (Iterable<User>) Collections.emptyIterator();
            list = emptyList;
        }
        return notNull(
            SparkyUtil.toList(notNull(list)).stream().map(UserStorageImpl::transformUser).collect(Collectors.toList())
        );
    }
}
