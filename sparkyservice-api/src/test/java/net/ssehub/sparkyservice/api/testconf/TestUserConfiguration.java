package net.ssehub.sparkyservice.api.testconf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import net.ssehub.sparkyservice.api.storeduser.IStoredUserService;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.StoredUserService;
import net.ssehub.sparkyservice.api.storeduser.UserRole;
import net.ssehub.sparkyservice.util.NullHelpers;

@TestConfiguration
public class TestUserConfiguration {

    @Bean(name = "storedUserDetailsService")
    @Primary
    public IStoredUserService iStoredUserService() {
        return new StoredUserService();
    }

    @Bean(name = "defaultUserService")
    public UserDetailsService defaultDetailsService() {
        return new TestUserDetailsService(UserRole.ADMIN);
    }
    
    @Bean(name = "adminUserService")
    public UserDetailsService adminDetailsService() {
        return new TestUserDetailsService(UserRole.DEFAULT);
    }

    static class TestUserDetailsService implements UserDetailsService {
        UserRole role;

        public TestUserDetailsService(UserRole role) {
            this.role = role;
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            var basicUser = StoredUserDetails.createStoredLocalUser(NullHelpers.notNull(username), "abcdefgh", true);
            basicUser.setRole(NullHelpers.notNull(role));
            return basicUser;
        }
    }
}
