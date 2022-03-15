package net.ssehub.sparkyservice.api.auth.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * Manages the login requests and returns users from {@link UserRealm#RECOVERY}.
 * 
 * @author marcel
 */
@Service
public class MemoryDetailsService implements UserDetailsService {

    @Value("${recovery.password:}")
    private String inMemoryPassword;

    @Value("${recovery.user:user}")
    private String inMemoryUser;

    @Autowired
    private PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username.equals(inMemoryUser)) {
            String encrPw = NullHelpers.notNull(encoder.encode(inMemoryPassword));
            var password = new Password(encrPw, encoder.getClass().getSimpleName());
            var memUser = UserRealm.RECOVERY.getUserFactory()
                    .create(inMemoryUser, password, UserRole.ADMIN, true);
            return memUser;
        }
        throw new UsernameNotFoundException(username + " not found");
    }
}