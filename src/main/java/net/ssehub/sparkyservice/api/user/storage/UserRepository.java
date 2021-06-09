package net.ssehub.sparkyservice.api.user.storage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Provides storage operations for {@link User}.
 *
 * @author marcel
 */
@Repository
interface UserRepository extends CrudRepository<User, Integer> {
    Optional<User> findByuserNameAndRealm(String username, UserRealm realm);
    Optional<List<User>> findByuserName(String username);
    Iterable<User> findAll();
    Iterable<User> findByRealm(UserRealm realm);
    Iterable<User> findByRole(UserRole role);
}
