package net.ssehub.sparkyservice.api.auth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.auth.storage.JwtRepository;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Provides test cases for {@link AdditionalAuthInterpreter}.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
public class AdditionalAuthInterepterTests {
    private static final String USER_NAME = "testuser";
    
    @Nonnull
    private final JwtSettings confValues;

    @Nonnull
    private final JwtTokenService jwtTokenService;

    @MockBean
    private JwtRepository mockedJwtRepo;
    
    private String jwtToken; 

    /**
     * Setup jwt settings.
     */
    public AdditionalAuthInterepterTests() {
        confValues = UnitTestDataConfiguration.sampleJwtConf();
        this.jwtTokenService = new JwtTokenService(confValues);
    }

    /**
     * Setup method creates a JWT token
     */
    @BeforeEach
    public void setUpConfValues() {
        var user = UserRealm.LDAP.getUserFactory().create(USER_NAME, null, UserRole.ADMIN, true);
        this.jwtToken = jwtTokenService.createFor(user);
    }

    /**
     * Test if the authentication reader returns a token object after decoding a token.
     */
    @Test
    @DisplayName("Test if token IDENT is extracted successful")
    public void positivTokenReturnTest() {
        var reader = new AdditionalAuthInterpreter(jwtTokenService, jwtToken);
        assertAll(
            () -> assertTrue(reader.getAuthenticatedUserIdent().isPresent()),
            () -> assertTrue((USER_NAME + "@ldap").equalsIgnoreCase(reader.getAuthenticatedUserIdent().get()))
        );
    }

    @Test
    @DisplayName("Authentication with null as header test")
    public void nullHeaderTest() {
        var reader = new AdditionalAuthInterpreter(jwtTokenService, null);
        assertTrue(reader.getAuthenticatedUserIdent().isEmpty(), "Actually there shouldn't be an authenticated user");
    }
}
