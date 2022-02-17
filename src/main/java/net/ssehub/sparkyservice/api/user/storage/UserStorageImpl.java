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
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.util.MiscUtil;

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
     * Finds user by a provided strategy. 
     * 
     * @author marcel
     */
    private static class UserFinder {
        private final SparkyUser user;

        /**
         * Finds user by a provided strategy.
         * 
         * @param user - The user to search for
         */
        public UserFinder(SparkyUser user) {
            this.user = user;
        }
        
        /**
         * Checks if a user is present with the provided search actions with error handling. 
         * 
         * @param userSearchAction - Methods which is used for searching a user; when one fails, the next one is used
         *                           The method can throw a {@link UserNotFoundException}
         * @return <code>true</code> When the user was found by any provided action
         */
        @SafeVarargs
        private final boolean checkForUser(Function<SparkyUser, SparkyUser>... userSearchAction) {
            return Stream.of(userSearchAction)
                .map(Function.identity())
                .collect(Collectors.toList())
                .stream()
                .dropWhile(this::invokeSingleAction)
                .count() > 0;
        }

        /**
         * Applys a single search action.
         * 
         * @param searchAction
         * @return <code>true</code> if search action finds the desired {@link #user}
         */
        private final boolean invokeSingleAction(Function<SparkyUser, SparkyUser> searchAction) {
            boolean found;
            try {
                found = Optional.ofNullable(user).map(searchAction).orElse(null) != null;
            } catch (UserNotFoundException e) {
                found = false;
            }
            return !found; //if not found, drop action and try next one
        }
    }

    /**
     * Creates the correct implementation of the provided user based on the realm.
     * 
     * @param user
     * @return SparkyUser representation of given JPA object
     */
    public @Nonnull static SparkyUser transformUser(@Nonnull User user) {
        return user.getRealm().getUserFactory().create(user);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public <T extends SparkyUser> void commit(@Nonnull T user) {
        try {
            User jpa = user.getJpa();
            log.debug("Try to store user {}@{} into database", jpa.getNickname(), jpa.getRealm());
            repository.save(jpa);
            log.debug("...stored");
        } catch (NoTransactionUnitException e) {
            log.debug("Don't safe user: {}", user.getUsername());
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
    public @Nonnull List<SparkyUser> findUsers(@Nullable String username) {
        return notNull(
            UserStorageImpl.validateUsername(username)
                .flatMap(repository::findByuserName)
                .orElseGet(ArrayList::new)
                .stream()
                .map(UserStorageImpl::transformUser)
                .collect(Collectors.toList())
            );
    }

    /**
     * Finds a specific user by name and realm.
     * 
     * @param username
     * @param realm
     * @return Specific user
     * @throws UserNotFoundException
     */
    private @Nonnull SparkyUser findUserByNameAndRealm(@Nullable String username, @Nullable UserRealm realm) 
            throws UserNotFoundException {
        return notNull(UserStorageImpl.validateUsername(username)
            .flatMap(name -> repository.findByuserNameAndRealm(name, realm))
            .map(UserStorageImpl::transformUser)
            .orElseThrow(
                () -> new UserNotFoundException(username + "@" + realm + " not found in storage")));
    } 

    /**
     * {@inheritDoc}.
     */
    @Override
    public @Nonnull SparkyUser findUser(int id) throws UserNotFoundException {
        Optional<User> user = repository.findById(id);
        var localUser = user.map(UserStorageImpl::transformUser)
                .orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return notNull(localUser);
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isUserInStorage(@Nullable SparkyUser user) {
        var filter = new UserFinder(user);
        boolean found = false;
        try {
            if (user != null) {
                found =  filter.checkForUser(
                    u -> this.findUser(u.getJpa().getId()), 
                    u -> this.findUser(u.getUsername())
                );
            } 
        } catch (NoTransactionUnitException e) {
            found = false;
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
            MiscUtil.toList(notNull(list)).stream().map(UserStorageImpl::transformUser).collect(Collectors.toList())
        );
    }
    
    /**
     * Validates a username.
     * 
     * @param username
     * @return Optional of username - has content when valid
     */
    public static @Nonnull Optional<String> validateUsername(@Nullable String username) {
        return notNull(Optional.ofNullable(username).map(String::toLowerCase));
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable String identName) throws UserNotFoundException {
        if (identName != null) {
            return findUser(Identity.of(identName));
        }
        throw new UserNotFoundException("Null");
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable Identity ident) throws UserNotFoundException {
        if (ident == null) {
            throw new UserNotFoundException("null");
        }
        return findUserByNameAndRealm(ident.nickname(), ident.realm());
    }
    
}
