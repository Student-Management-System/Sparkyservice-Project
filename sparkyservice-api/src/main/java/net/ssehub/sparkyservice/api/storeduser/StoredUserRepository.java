package net.ssehub.sparkyservice.api.storeduser;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import net.ssehub.sparkyservice.db.user.StoredUser;

@Repository
public interface StoredUserRepository extends CrudRepository<StoredUser, Integer>{
    Optional<StoredUser> findByuserNameAndRealm(String username, String realm);
    Optional<List<StoredUser>> findByuserName(String username);
}
