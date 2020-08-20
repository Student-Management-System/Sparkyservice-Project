package net.ssehub.sparkyservice.api.auth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.creation.UserFactoryProvider;

public class AuthenticationReaderTests {
    private static final String USER_NAME = "testuser";
    
    @Nonnull
    private final JwtSettings confValues = new JwtSettings();

    private String jwtToken; 

    public AuthenticationReaderTests() {
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        ReflectionTestUtils.setField(confValues, "secret", secretString);
        ReflectionTestUtils.setField(confValues, "type", "Bearer");
        ReflectionTestUtils.setField(confValues, "issuer", "TestUnit");
        ReflectionTestUtils.setField(confValues, "audience", "Y");
    }

    /**
     * Setup method creates a JWT token
     */
    @BeforeEach
    public void setUpConfValues() {
        var user = UserFactoryProvider.getFactory(UserRealm.LDAP).create(USER_NAME, null, UserRole.ADMIN, true);
        this.jwtToken = JwtAuth.createJwtToken(user, confValues);
    }

    /**
     * Test if the authentication reader returns a token object after decoding a token.
     */
    @Test
    @DisplayName("Test if token IDENT is extracted successful")
    public void positivTokenReturnTest() {
        var reader = new AuthenticationReader(confValues, jwtToken);
        assertAll(
            () -> assertTrue(reader.getAuthenticatedUserIdent().isPresent()),
            () -> assertTrue(
                    (USER_NAME + "@ldap").equalsIgnoreCase(reader.getAuthenticatedUserIdent().get()))
         );
    }

    @Test
    @DisplayName("Testing authentication reader null token")
    public void nullHeaderTest() {
        var reader = new AuthenticationReader(confValues, null);
        assertTrue(reader.getAuthenticatedUserIdent().isEmpty(), "Actually there shouldn't be an authenticated user");
    }
}
