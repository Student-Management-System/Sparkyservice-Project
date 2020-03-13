package net.ssehub.sparkyservice.api.storeduser;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.userdetails.UserDetailsService;

import net.ssehub.sparkyservice.db.user.StoredUser;

public interface IStoredUserService extends UserDetailsService {
    
    <T extends StoredUser> void storeUser(@Nonnull T user);
    
    StoredUser findUserByid(int id) throws UserNotFoundException;
    List<StoredUser> findUsersByUsername(@Nullable String username) throws UserNotFoundException;
    StoredUser findUserByNameAndRealm(@Nullable String username,@Nullable String realm) throws UserNotFoundException;
}
