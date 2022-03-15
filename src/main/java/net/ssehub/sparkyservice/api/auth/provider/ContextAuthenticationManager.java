package net.ssehub.sparkyservice.api.auth.provider;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;

@Component
public class ContextAuthenticationManager {
    
    @Autowired
    private AuthenticationManager fullAuthenticationManager;
    
    @Autowired(required = false) 
    @Qualifier("localDbAuthProvider")
    private AuthenticationProvider localAuthProvider;
    
    @Autowired(required = false) 
    @Qualifier("memoryAuthProvider")
    private AuthenticationProvider memoryAuthProvider;
    
    @Autowired(required = false) 
    @Qualifier("adLdapAuthProvider")
    private AuthenticationProvider adLdapAuthProvider;

    public AuthenticationManagerResolver<HttpServletRequest> requestContextResolver() {
        return request -> realmBasedManager(request.getParameter("username"));
    }

    public AuthenticationManagerResolver<CredentialsDto> credentialsContextResolver() {
        return credentials -> realmBasedManager(credentials.username);
    }
    
    private AuthenticationManager realmBasedManager(String username) {
        try {
            switch (Identity.of(username).realm()) {
            case LOCAL:
                return getManagerNotNull(localAuthProvider);
            case RECOVERY:
                return getManagerNotNull(memoryAuthProvider);
            case UNIHI:
                return getManagerNotNull(adLdapAuthProvider);
            default:
                return fullAuthenticationManager;
            }
        } catch (IllegalArgumentException e) {
            return fullAuthenticationManager;
        }
    }

    private static AuthenticationManager getManagerNotNull(AuthenticationProvider provider) {
        if (provider != null) {
            return authentication -> removeRealmFromNicknameAttempt(provider, authentication);
        } else {
            throw new AuthenticationException();
        }
    }

    protected static Authentication removeRealmFromNicknameAttempt(AuthenticationProvider provider,
            Authentication authentication) {
        String username = authentication.getName();
        try {
            var ident = Identity.of(username);
            var tokenNicknameOnly = new UsernamePasswordAuthenticationToken(ident.nickname(), 
                    authentication.getCredentials());
            return provider.authenticate(tokenNicknameOnly);
        } catch (IllegalArgumentException e) {
            return provider.authenticate(authentication);
        }
    }
}
