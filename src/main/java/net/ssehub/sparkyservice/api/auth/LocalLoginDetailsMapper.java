//package net.ssehub.sparkyservice.api.auth;
//
//import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
//
//import java.util.Optional;
//
//import javax.annotation.Nonnull;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import net.ssehub.sparkyservice.api.user.LocalUserDetails;
//import net.ssehub.sparkyservice.api.user.UserRealm;
//import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
//import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
//
///**
// * Manages the login request to a local storage and returns users from {@link UserRealm#LOCAL}. 
// * 
// * @author marcel
// */
//@Service
//public class LocalLoginDetailsMapper implements UserDetailsService {
//    
//    @Autowired
//    private UserStorageService storageService;
//
//    /**
//     * Is used by SpringSecurity for getting user details with a given username. It returns a single UserDetails without
//     * limiting the search to a specific realm. Through this, a specific realm is always preferred (typically the realm
//     * which is used for local authentication).
//     * 
//     * @param username
//     *                 name to look for
//     * @return userDetails Details loaded from a data storage which is identified by the given username
//     * @throws When
//     *              a the given username is not found in storage (spring will continue with the next configured
//     *              {@link AuthenticationProvider})
//     */
//    @Override
//    @Nonnull
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        try {            
//            return notNull(
//                Optional.ofNullable(username)
//                    .map(name -> storageService.findUserByIdentity(new Identity(username)))
//                    .filter(LocalUserDetails.class::isInstance) // safety check!
//                    .orElseThrow(() -> new UsernameNotFoundException("User with name \"null\" not found"))
//            );
//        } catch (UserNotFoundException e) {
//            throw new UsernameNotFoundException(e.getMessage());
//        }
//    }
//
//}
