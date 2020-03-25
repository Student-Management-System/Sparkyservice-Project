package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.GrantedAuthority;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.UserRole;

public class JwtAuthTests {
    private final ConfigurationValues confValues = new ConfigurationValues();

    @BeforeEach
    public void setUpConfValues() {
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        ReflectionTestUtils.setField(confValues, "jwtSecret", secretString);
        ReflectionTestUtils.setField(confValues, "jwtTokenType", "Bearer");
        ReflectionTestUtils.setField(confValues, "jwtTokenIssuer", "TestUnit");
        ReflectionTestUtils.setField(confValues, "jwtTokenAudience", "Y");
    }

    public UserDetails createTestUserDetails() {
        List<SimpleGrantedAuthority> authy = new ArrayList<SimpleGrantedAuthority>();
        authy.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new User("TestUser", "TestPassword", authy);
    }

    @Test
    public void tokenUserDetailsTest() {
        String token = JwtAuth.createJwtToken(createTestUserDetails(), confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getJwtSecret());
        assertNotNull(authTokenNull);
    }

    @Test
    public void fullTokenStoredUserDetailsTest()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            AssertionError, NoSuchMethodException, SecurityException {
        Constructor<StoredUserDetails> constructor = StoredUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        StoredUserDetails user = notNull(constructor.newInstance());
        user.setRealm("DEFAULT");;
        user.setUserName("TestUser");
        user.setRole(UserRole.ADMIN);
        String token = JwtAuth.createJwtTokenWithRealm(user, confValues, user.getRealm());
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getJwtSecret());
        
        assertNotNull(authTokenNull);
        var authToken = notNull(authTokenNull);
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertAll(
                () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
                () -> assertEquals("TestUser", ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
                () -> assertEquals("DEFAULT", ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm()),
                () -> assertEquals(UserRole.ADMIN.name(), tokenAuthy.getAuthority())
            );
    }

    @Test
    public void tokenPrincipalTest() {
        String token = JwtAuth.createJwtToken(createTestUserDetails(), confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getJwtSecret());
        assumeTrue(authTokenNull != null);
        var authToken = notNull(authTokenNull);

        assertAll(
                () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
                () -> assertEquals("TestUser", ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
                () -> assertEquals("", ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm())
            );
    }

    @Test
    public void tokenRoleTest() {
        String token = JwtAuth.createJwtToken(createTestUserDetails(), confValues);
        UsernamePasswordAuthenticationToken authTokenNull = JwtAuth.readJwtToken(token, confValues.getJwtSecret());
        assumeTrue(authTokenNull != null);
        var authToken = notNull(authTokenNull);

        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertEquals(tokenAuthy.getAuthority(), "ROLE_ADMIN");
    }
}