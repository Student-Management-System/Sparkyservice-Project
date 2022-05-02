package net.ssehub.sparkyservice.api.auth.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.IllegalIdentityFormat;
import net.ssehub.sparkyservice.api.user.NoSuchRealmException;
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
        
        public MultiProviderAuthManager(Collection<AuthenticationProvider> providers) {
            this.providers = providers.toArray(AuthenticationProvider[]::new);
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
            } catch (IllegalIdentityFormat | NoSuchRealmException e) {
                return auth;
            }
        }
        
    }
    
    private List<UserRealm> sortedProviderConfigs;
    
    public ContextAuthenticationManager(List<UserRealm> provider) {
        super();
        this.sortedProviderConfigs = provider.stream().sorted(this::providerSortStrategy).collect(Collectors.toList());
    }

    @Bean
    public AuthenticationManagerResolver<CredentialsDto> credentialsContextResolver() {
        return credentials -> userManager(credentials.username);
    }
    
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> requestContextResolver() {
        return request -> userManager(request.getParameter("username"));
    }
    
    public AuthenticationManager globalManager() {
        var sortedProvider = sortedProviderConfigs.stream()
                .map(UserRealm::authenticationProvider)
                .collect(Collectors.toList());
        return auth -> new MultiProviderAuthManager(sortedProvider).authenticate(auth);
    }

    @Bean
    public AuthenticationManagerResolver<UserRealm> realmContextResolver() {
        return realm -> {
            var foundProviders = sortedProviderConfigs.stream()
                    .filter(realm::equals)
                    .map(UserRealm::authenticationProvider)
                    .collect(Collectors.toList());
            return new MultiProviderAuthManager(foundProviders);
        };
    }

    private AuthenticationManager userManager(String username) {
        try {
            return realmContextResolver().resolve(Identity.of(username).realm());
        } catch (NoSuchRealmException | IllegalIdentityFormat e) {
            return globalManager();
        }
    }
    
    private int providerSortStrategy(UserRealm first, UserRealm second) {
        return first.authenticationPriorityWeight() - second.authenticationPriorityWeight();
    }
}
