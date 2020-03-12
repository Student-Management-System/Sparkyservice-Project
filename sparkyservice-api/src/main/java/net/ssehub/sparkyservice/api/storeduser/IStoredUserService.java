package net.ssehub.sparkyservice.api.storeduser;

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.security.core.userdetails.UserDetailsService;

import net.ssehub.sparkyservice.db.user.StoredUser;

public interface IStoredUserService extends UserDetailsService {
    
    <T extends StoredUser> void storeUser(@Nonnull T user);
    
    StoredUser findUserByid(int id) throws UserNotFoundException;
    List<StoredUser> findUsersByUsername(@Nonnull String username) throws UserNotFoundException;
    StoredUser findUserByNameAndRealm(@Nonnull String username, @Nonnull String realm) throws UserNotFoundException;
}
