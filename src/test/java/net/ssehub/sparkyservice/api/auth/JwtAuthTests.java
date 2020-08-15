package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;

/**
 * Tests for {@link JwtAuth} class. 
 * 
 * @author marcel
 */
public class JwtAuthTests {
    private static final String USERNAME = "TESTUSER";
    private static final @Nonnull UserRole AUTHORITY = UserRole.ADMIN;

    private final JwtSettings confValues = new JwtSettings();

    @BeforeEach
    public void setUpConfValues() {
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        ReflectionTestUtils.setField(confValues, "secret", secretString);
        ReflectionTestUtils.setField(confValues, "type", "Bearer");
        ReflectionTestUtils.setField(confValues, "issuer", "TestUnit");
        ReflectionTestUtils.setField(confValues, "audience", "Y");
    }

    @Test
    public void tokenUserDetailsTest() {
        User user = LocalUserDetails.newLocalUser(USERNAME, "", AUTHORITY);
        user.setRealm(UserRealm.UNKNOWN);
        String token = JwtAuth.createJwtToken(user, confValues);
        var authToken = JwtAuth.readJwtToken(token, confValues.getSecret());
        assertTrue(authToken.isPresent());
    }

    @Test
    public void fullTokenStoredUserDetailsTest()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            AssertionError, NoSuchMethodException, SecurityException {
        Constructor<LocalUserDetails> constructor = LocalUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        LocalUserDetails user = notNull(constructor.newInstance());
        user.setRealm(UserRealm.LOCAL);
        user.setUserName(USERNAME);
        user.setRole(UserRole.ADMIN);
        String token = JwtAuth.createJwtToken(user, confValues);
        UsernamePasswordAuthenticationToken authToken = JwtAuth.readJwtToken(token, confValues.getSecret())
                .orElseThrow(() -> new IllegalArgumentException("could not read jwt token"));
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertAll(
            () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
            () -> assertEquals(USERNAME, ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
            () -> assertEquals(LocalUserDetails.DEFAULT_REALM, 
                    ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm()),
            () -> assertEquals(UserRole.FullName.ADMIN, tokenAuthy.getAuthority())
        );
    }

    @Test
    public void tokenPrincipalTest() {
        User user = LocalUserDetails.newLocalUser(USERNAME, "", AUTHORITY);
        user.setRealm(UserRealm.UNKNOWN);
        String token = JwtAuth.createJwtToken(user, confValues);
        UsernamePasswordAuthenticationToken authToken = JwtAuth.readJwtToken(token, confValues.getSecret())
                .orElseThrow(() -> new IllegalArgumentException("could not read jwt token"));
        assertAll(
            () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
            () -> assertEquals(USERNAME, ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
            () -> assertEquals(UserRealm.UNKNOWN, ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm())
        );
    }

    @Test
    public void tokenRoleTest() {
        User user = LocalUserDetails.newLocalUser(USERNAME, "", AUTHORITY);
        user.setRealm(UserRealm.UNKNOWN);
        String token = JwtAuth.createJwtToken(user, confValues);
        UsernamePasswordAuthenticationToken authToken = JwtAuth.readJwtToken(token, confValues.getSecret())
                .orElseThrow(() -> new IllegalArgumentException("could not read jwt token"));
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertEquals(tokenAuthy.getAuthority(), "ROLE_ADMIN");
    }
}
