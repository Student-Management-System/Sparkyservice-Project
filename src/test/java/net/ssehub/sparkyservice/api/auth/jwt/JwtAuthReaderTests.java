package net.ssehub.sparkyservice.api.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtRepository;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Provides test cases for {@link JwtAuthReader}.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@Import(JwtTestBeanConf.class)
public class JwtAuthReaderTests {
    private static final String USER_NAME = "testuser";
    
    @Autowired
    @Nonnull
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtAuthReader reader;
    
    @MockBean
    private JwtRepository mockedJwtRepo;
    
    private String jwtToken; 
    
    private SparkyUser user;

    /**
     * Setup method creates a JWT token
     */
    @BeforeEach
    public void setUpConfValues() {
        var user = UserRealm.UNIHI.getUserFactory().create(USER_NAME, null, UserRole.ADMIN, true);
        this.user = user;
        this.jwtToken = jwtTokenService.createFor(user);
    }

    /**
     * Test if the authentication reader returns a token object after decoding a token.
     */
    @Test
    @DisplayName("Test if token IDENT is extracted successful")
    public void positivTokenReturnTest() {
        assertAll(
            () -> assertTrue(reader.getAuthenticatedUserIdent(jwtToken).isPresent()),
            () -> assertEquals(user.getIdentity(), reader.getAuthenticatedUserIdent(jwtToken).get())
        );
    }

    @Test
    @DisplayName("Authentication with null as header test")
    public void nullHeaderTest() {
        assertTrue(reader.getAuthenticatedUserIdent(null).isEmpty(), "There shouldn't be an authenticated user");
    }
    
    
}
