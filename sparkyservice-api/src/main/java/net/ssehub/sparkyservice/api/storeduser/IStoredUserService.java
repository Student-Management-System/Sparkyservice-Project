package net.ssehub.sparkyservice.api.storeduser;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;

import net.ssehub.sparkyservice.db.user.StoredUser;

public interface IStoredUserService extends UserDetailsService {
    StoredUser findUserByid(int id) throws UserNotFoundException;
    List<StoredUser> findUserByUsername(String username) throws UserNotFoundException;
    Boolean storeNewUser(NewUserDto newUser);
    StoredUser findByuserNameAndRealm(String username, String realm) throws UserNotFoundException;
}
