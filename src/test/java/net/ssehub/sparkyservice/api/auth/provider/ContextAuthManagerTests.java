package net.ssehub.sparkyservice.api.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

public class ContextAuthManagerTests {
    
    static class DummyAuthenticationProvider implements AuthenticationProvider {

        @Override
        public boolean supports(Class<?> authentication) {
            return true;
        }
        
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return new UsernamePasswordAuthenticationToken(authentication.getName(),
                    authentication.getCredentials(), List.of(UserRole.ADMIN));
        }
        
    }

    @ParameterizedTest
    @EnumSource(value = UserRealm.class, names = {"LOCAL", "RECOVERY", "UNIHI"})
    public void testRemoveRealmFromUsername(@Nonnull UserRealm realm) {
        var ident = new Identity("test", realm);
        var attemptAuthentication = new UsernamePasswordAuthenticationToken(ident.asUsername(), "password");
        var newAuthentication = ContextAuthenticationManager.removeRealmFromNicknameAttempt(new DummyAuthenticationProvider(), attemptAuthentication);
        assertEquals(ident.nickname(), newAuthentication.getName());
    }
    
    @Test
    public void testNonRemove() {
        var attemptAuthentication = new UsernamePasswordAuthenticationToken("test", "password");
        var newAuthentication = ContextAuthenticationManager.removeRealmFromNicknameAttempt(new DummyAuthenticationProvider(), attemptAuthentication);
        assertEquals("test", newAuthentication.getName());
    }
}
