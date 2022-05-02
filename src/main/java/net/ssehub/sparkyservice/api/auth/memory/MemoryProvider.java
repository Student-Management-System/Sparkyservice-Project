package net.ssehub.sparkyservice.api.auth.memory;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.auth.StorageDetailsService;
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.useraccess.UserRole;

@Configuration
@ParametersAreNonnullByDefault
class MemoryProvider {

    @Autowired
    private PasswordEncoder pwEncoder;

    @ConditionalOnProperty(value = "recovery.enabled", havingValue = "true")
    @Bean("memoryAuthProvider")
    public AuthenticationProvider memoryAuthProvider(StorageDetailsService detailsService, MemoryRealm realm) {
        var prov = new DaoAuthenticationProvider();
        UserDetailsService memoryService = nickname -> detailsService.loadUser(nickname, realm);
        prov.setUserDetailsService(memoryService);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }

    @Bean
    public MemoryUser memoryUsers(@Value("${recovery.password:}") String password,
        @Value("${recovery.user:user}") String user, MemoryRealm realm, PasswordEncoder encoder) {
        var p = new Password(notNull(encoder.encode(password)));
        return new MemoryUser(user, realm, p, UserRole.ADMIN);
    }
}
