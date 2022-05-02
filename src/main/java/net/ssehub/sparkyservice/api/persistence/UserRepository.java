package net.ssehub.sparkyservice.api.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.ssehub.sparkyservice.api.persistence.jpa.user.User;
import net.ssehub.sparkyservice.api.useraccess.UserRole;

/**
 * Provides storage operations for {@link User}.
 *
 * @author marcel
 */
@Repository
interface UserRepository extends CrudRepository<User, Integer> {
    //CHECKSTYLE:OFF
    Optional<User> findByuserNameAndRealm(String username, String realmIdentifier);
    Optional<List<User>> findByuserName(String username);
    Iterable<User> findAll();
    Iterable<User> findByRealm(String realmIdentifier);
    Iterable<User> findByRole(UserRole role);
    //CHECKSTYLE:ON
}
