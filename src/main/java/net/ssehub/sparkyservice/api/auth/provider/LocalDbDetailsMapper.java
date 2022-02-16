package net.ssehub.sparkyservice.api.auth.provider;
//package net.ssehub.sparkyservice.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Manages the login request to a local storage and returns users from {@link UserRealm#LOCAL}. 
 * 
 * @author marcel
 */
@Service
public class LocalDbDetailsMapper implements UserDetailsService {
    
    @Autowired
    private UserStorageService storageService;

    /**
   * Is used by SpringSecurity for getting user details with a given username. It returns a single UserDetails without
   * limiting the search to a specific realm. Through this, a specific realm is always preferred (typically the realm
     * which is used for local authentication).
     * 
     * @param username
     *                 name to look for
     * @return userDetails Details loaded from a data storage which is identified by the given username
     * @throws When
     *              a the given username is not found in storage (spring will continue with the next configured
     *              {@link AuthenticationProvider})
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("null");
        }
        try {            
            var ident = new Identity(username, LocalUserDetails.DEFAULT_REALM);
            return storageService.findUser(ident);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

}
