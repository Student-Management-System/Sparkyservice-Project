package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.GrantedAuthority;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;

public class JwtAuthTests {
    private final JwtSettings confValues = new JwtSettings();

    private static final String USERNAME = "TESTUSER";
    private static final List<UserRole> authority = Arrays.asList(UserRole.ADMIN);

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
        String token = JwtAuth.createJwtToken(USERNAME, authority, confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getSecret());
        assertNotNull(authTokenNull);
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
        String token = JwtAuth.createJwtTokenWithRealm(USERNAME, authority, confValues, user.getRealm());
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getSecret());
        
        assertNotNull(authTokenNull);
        var authToken = notNull(authTokenNull);
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertAll(
                () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
                () -> assertEquals(USERNAME, ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
                () -> assertEquals(LocalUserDetails.DEFAULT_REALM, ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm()),
                () -> assertEquals(UserRole.FullName.ADMIN, tokenAuthy.getAuthority())
            );
    }

    @Test
    public void tokenPrincipalTest() {
        String token = JwtAuth.createJwtToken(USERNAME, authority, confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getSecret());
        assumeTrue(authTokenNull != null);
        var authToken = notNull(authTokenNull);

        assertAll(
                () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
                () -> assertEquals(USERNAME, ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
                () -> assertEquals(UserRealm.UNKNOWN, ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm())
            );
    }

    @Test
    public void tokenRoleTest() {
        String token = JwtAuth.createJwtToken(USERNAME, authority, confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getSecret());
        assumeTrue(authTokenNull != null);
        var authToken = notNull(authTokenNull);

        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertEquals(tokenAuthy.getAuthority(), "ROLE_ADMIN");
    }
}
