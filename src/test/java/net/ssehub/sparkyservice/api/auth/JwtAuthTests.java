package net.ssehub.sparkyservice.api.auth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.MemoryUser;
import net.ssehub.sparkyservice.api.user.SparkyUser;

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

    /**
     * Tests for token creation and if this token can be read afterwards.
     * When this tests fails, the token creation is invalid or the token creation has an error. 
     * <br>
     * (Because of the strong coupling between those two methods, they have to checked against each other).
     */
    @Test
    public void tokenUserDetailsTest() {
        SparkyUser user = new MemoryUser(USERNAME, new Password("test", "plain"), AUTHORITY);
        String token = JwtAuth.createJwtToken(user, confValues);
        assertDoesNotThrow(() -> JwtAuth.readJwtToken(token, confValues.getSecret()));
    }

    /**
     * Test for token creation. Test if all information are available in the returned 
     * {@link UsernamePasswordAuthenticationToken}. 
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws AssertionError
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws JwtTokenReadException
     */
    @Test
    public void fullTokenStoredUserDetailsTest()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            AssertionError, NoSuchMethodException, SecurityException, JwtTokenReadException {
        SparkyUser user = LocalUserDetails.newLocalUser(USERNAME, "", AUTHORITY);
        String token = JwtAuth.createJwtToken(user, confValues);
        
        UsernamePasswordAuthenticationToken authToken = JwtAuth.readJwtToken(token, confValues.getSecret());
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertAll(
            () -> assertTrue(authToken.getPrincipal() instanceof SparkysAuthPrincipal),
            () -> assertEquals(USERNAME, ((SparkysAuthPrincipal) authToken.getPrincipal()).getName()),
            () -> assertEquals(UserRealm.LOCAL, ((SparkysAuthPrincipal) authToken.getPrincipal()).getRealm()),
            () -> assertEquals(UserRole.FullName.ADMIN, tokenAuthy.getAuthority())
        );
    }

    @Test
    public void tokenRoleTest() throws JwtTokenReadException {
        SparkyUser user = LocalUserDetails.newLocalUser(USERNAME, "", AUTHORITY);
        String token = JwtAuth.createJwtToken(user, confValues);
        UsernamePasswordAuthenticationToken authToken = JwtAuth.readJwtToken(token, confValues.getSecret());
        var tokenAuthy = (GrantedAuthority) authToken.getAuthorities().toArray()[0];
        assertEquals(tokenAuthy.getAuthority(), "ROLE_ADMIN");
    }

    /**
     * Test if the correct exception is thrown when an empty token is given.
     */
    @Test
    public void testReadNullFail() {
        assertThrows(JwtTokenReadException.class, () -> readJwtToken(null));
    }

//    @Test TODO make JWtAuth Method with expiration strategy
//    public void testExpiredTokenFail() {
//        assertThrows(JwtTokenReadException.class, () -> readJwtToken(null));
//    }

    @SuppressWarnings("null")
    public UsernamePasswordAuthenticationToken readJwtToken(String token) throws JwtTokenReadException {
        return JwtAuth.readJwtToken(token, confValues.getSecret());
    }
}
