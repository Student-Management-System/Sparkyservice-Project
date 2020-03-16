package net.ssehub.sparkyservice.api.storeduser;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.userdetails.UserDetailsService;

import net.ssehub.sparkyservice.db.user.StoredUser;

public interface IStoredUserService extends UserDetailsService {
    
    <T extends StoredUser> void storeUser(@Nonnull T user);
    
    StoredUserDetails findUserById(int id) throws UserNotFoundException;
    List<StoredUserDetails> findUsersByUsername(@Nullable String username) throws UserNotFoundException;
    StoredUserDetails findUserByNameAndRealm(@Nullable String username,@Nullable String realm) throws UserNotFoundException;
    boolean userExistsInDatabase(@Nullable StoredUser user);
}
