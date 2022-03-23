package net.ssehub.sparkyservice.api.auth.provider;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import net.ssehub.sparkyservice.api.auth.provider.ProviderConfig.SparkyProvider;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;

@Configuration
public class ContextAuthenticationManager {
    
    static class MultiProviderAuthManager implements AuthenticationManager {
        private AuthenticationProvider[] providers;
        
        /**
         * Defines a Authentication Manager for multiple 
         * @param providers
         */
        public MultiProviderAuthManager(AuthenticationProvider... providers) {
            this.providers = providers;
        }
        
        public MultiProviderAuthManager(SparkyProvider... providers) {
            this.providers = providers;
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            BadCredentialsException lastException = new BadCredentialsException("Authentication failed");
            for (var p : Optional.ofNullable(providers).orElse(new AuthenticationProvider[0])) {
                try {
                    if (p != null) {
                        var executedAuthAttempt = p.authenticate(removeRealmFromNicknameAttempt(authentication));
                        if (executedAuthAttempt.isAuthenticated()) {
                            return executedAuthAttempt;
                        }
                    }
                } catch (BadCredentialsException e) {
                    lastException = e;           
                }
            }
            throw lastException;
        }

        protected static Authentication removeRealmFromNicknameAttempt(Authentication auth) {
            String username = auth.getName();
            try {
                var ident = Identity.of(username);
                return new UsernamePasswordAuthenticationToken(ident.nickname(), auth.getCredentials());
            } catch (IllegalArgumentException e) {
                return auth;
            }
        }
        
    }
    
    @Autowired
    private List<SparkyProvider> provider;

    @Bean
    public AuthenticationManagerResolver<CredentialsDto> credentialsContextResolver() {
        return credentials -> userManager(credentials.username);
    }
    
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> requestContextResolver() {
        return request -> userManager(request.getParameter("username"));
    }
    
    public AuthenticationManager globalManager() {
        var sortedProvider = provider.stream().sorted(this::providerSortStrategy).toArray(SparkyProvider[]::new);
        return auth -> new MultiProviderAuthManager(sortedProvider).authenticate(auth);
    }

    @Bean
    public AuthenticationManagerResolver<UserRealm> realmContextResolver() {
        return realm -> {
            var foundProviders = provider.stream()
                    .filter(po -> po.supports(realm))
                    .sorted(this::providerSortStrategy)
                    .toArray(SparkyProvider[]::new);
            return new MultiProviderAuthManager(foundProviders);
        };
    }

    private AuthenticationManager userManager(String username) {
        try {
            return realmContextResolver().resolve(Identity.of(username).realm());
        } catch (IllegalArgumentException e) {
            return globalManager();
        }
    }

    
    private int providerSortStrategy(SparkyProvider first, SparkyProvider second) {
        return first.getWeight() - second.getWeight();
    }
}
