package net.ssehub.sparkyservice.api.testconf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.util.NullHelpers;

@TestConfiguration
public class TestUserConfiguration {

    @Bean(name = "defaultUserService")
    public UserDetailsService defaultDetailsService() {
        return new TestUserDetailsService(UserRole.DEFAULT);
    }
    
    @Bean(name = "adminUserService")
    public UserDetailsService adminDetailsService() {
        return new TestUserDetailsService(UserRole.ADMIN);
    }

    static class TestUserDetailsService implements UserDetailsService {
        UserRole role;

        @Autowired
        private UserStorageService service;

        public TestUserDetailsService(UserRole role) {
            this.role = role;
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            var basicUser = LocalUserDetails.newLocalUser(NullHelpers.notNull(username), "abcdefgh", 
                    NullHelpers.notNull(role));
            service.commit(basicUser);
            return basicUser;
        }
    }
}
