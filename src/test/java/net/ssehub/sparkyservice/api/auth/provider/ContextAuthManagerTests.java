package net.ssehub.sparkyservice.api.auth.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;

public class ContextAuthManagerTests {

    @ParameterizedTest
    @EnumSource(value = UserRealm.class, names = { "LOCAL", "RECOVERY", "UNIHI" })
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
