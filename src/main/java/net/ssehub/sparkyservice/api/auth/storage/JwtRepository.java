package net.ssehub.sparkyservice.api.auth.storage;

import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.ssehub.sparkyservice.api.jpa.token.JpaJwtToken;
import net.ssehub.sparkyservice.api.jpa.user.User;

@Repository
public interface JwtRepository extends CrudRepository<JpaJwtToken, String> {
    Iterable<JpaJwtToken> findAll();

    List<JpaJwtToken> findByUser(User user);

    Set<JpaJwtToken> findByLocked(boolean nonLocked);
}
