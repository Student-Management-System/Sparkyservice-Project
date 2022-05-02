package net.ssehub.sparkyservice.api.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import net.ssehub.sparkyservice.api.testconf.TestSetupMethods;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;

public class ContextAuthManagerTests {
    
    static Stream<Arguments> testRemoveRealmFromUsername() {
        return TestSetupMethods.allTestRealmSetup().stream().map(Arguments::of);
    }
    
    @ParameterizedTest
    @MethodSource
    public void testRemoveRealmFromUsername(@Nonnull UserRealm realm) {
        var ident = new Identity("test", realm);
        var attemptAuthentication = new UsernamePasswordAuthenticationToken(ident.asUsername(), "password");
        var newAuthentication = ContextAuthenticationManager.MultiProviderAuthManager
                .removeRealmFromNicknameAttempt(attemptAuthentication);
        assertEquals(ident.nickname(), newAuthentication.getName());
    }

    @Test
    public void testNonRemove() {
        var attemptAuthentication = new UsernamePasswordAuthenticationToken("test", "password");
        var newAuthentication = ContextAuthenticationManager.MultiProviderAuthManager
                .removeRealmFromNicknameAttempt(attemptAuthentication);
        assertEquals("test", newAuthentication.getName());
    }
}
