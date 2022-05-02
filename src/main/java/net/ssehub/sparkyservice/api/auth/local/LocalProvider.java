package net.ssehub.sparkyservice.api.auth.local;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.auth.StorageDetailsService;

@Lazy
@Configuration
@ParametersAreNonnullByDefault
class LocalProvider {

    @Autowired
    private PasswordEncoder pwEncoder;
    
    @Bean("localDbAuthProvider")
    public AuthenticationProvider localDbAuthProvider(StorageDetailsService detailsService, LocalRealm realm) {
        var prov = new DaoAuthenticationProvider();
        UserDetailsService dbService = nickname -> detailsService.loadUser(nickname, realm);
        prov.setUserDetailsService(dbService);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }
}
