package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides unit tests for {@link JwtUtils}.
 * 
 * @author marcel
 */
public class JwtUtilsTests {

    private JwtToken testToken; 

    private final JwtSettings confValues = new JwtSettings();
    
    /**
     * Setup a test {@link #testToken} which can be used during tests. 
     */
    @BeforeEach
    public void setupTestToken() {
        var expDate = DateUtil.toUtilDate(LocalDate.now().plusDays(10));
        var id = new Identity("user", UserRealm.LOCAL);
        testToken = new JwtToken(notNull(UUID.randomUUID()), expDate, id.asUsername(), UserRole.ADMIN);
    }

    /**
     * Setup {@link #confValues}. 
     */
    @BeforeEach
    public void setupConfValues() {
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        ReflectionTestUtils.setField(confValues, "secret", secretString);
        ReflectionTestUtils.setField(confValues, "type", "Bearer");
        ReflectionTestUtils.setField(confValues, "issuer", "TestUnit");
        ReflectionTestUtils.setField(confValues, "audience", "Y");
    }

    /**
     * Tests if information can be encoded and decoded afterwards. 
     * Due strong coupling between those two methods, they're always tested together.
     */
    @Test
    @DisplayName("Test successful encoding and decoding")
    public void testEncodeTest() {
        String jwtString = JwtUtils.encode(testToken, confValues);
        assertDoesNotThrow(() -> JwtUtils.decodeAndExtract(jwtString, confValues.getSecret()));
    }

    /**
     * Tests if values are decoded and be accessible after decoding. 
     */
    @Test
    public void testEncodingValuesTest() {
        String jwtString = JwtUtils.encode(testToken, confValues);
        JwtToken token = JwtUtils.decodeAndExtract(jwtString, confValues.getSecret());
        assertAll(
            () -> assertNotNull(token.getExpirationDate()),
            () -> assertNotNull(token.getRemainingRefreshes()),
            () -> assertNotNull(token.getJti()),
            () -> assertNotNull(token.getSubject())
        );
    }

}