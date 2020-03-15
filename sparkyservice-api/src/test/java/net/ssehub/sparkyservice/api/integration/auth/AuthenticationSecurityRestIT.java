package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.ssehub.sparkyservice.api.auth.AuthController;
import net.ssehub.sparkyservice.api.auth.JwtAuthenticationFilter;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class AuthenticationSecurityRestIT {
    
    @Autowired
    private WebApplicationContext context;

    @Value("${recovery.enabled}")
    private String inMemoryEnabled;
    
    @Value("${recovery.password}")
    private String inMemoryPassword;
    
    @Value("${recovery.user}")
    private String inMemoryUser;
    
    @Value("${jwt.token.header}")
    private String jwtTokenHeader;
    
    @Value("${jwt.token.prefix}")
    private String jwtTokenPrefix;
    
    private MockMvc mvc;
    
    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
          .webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }
    
    @Test
    @Disabled("First draft")
    public void localDatabaseAuthTest() throws Exception {
        
    }

    /**
     * Test for {@link AuthController#authenticate(String, String)}. <br>
     * Tests the security configuration if the authentication site is accessible for guests. It ignores other errors
     * like Internal Server Errors (the whole 5XX Status group). 
     * 
     * @throws Exception
     */
    @Test
    public void securityGuestAuthAccesabilityTest() throws Exception {
        MvcResult result = this.mvc
                .perform(
                    post(ConfigurationValues.AUTH_LOGIN_URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.TEXT_PLAIN))
                .andReturn();
        int resultStatus = result.getResponse().getStatus();
        assertAll(
                () -> assertNotEquals(404, resultStatus, "Authentication site was not found"),
                () -> assertNotEquals(403, resultStatus, "Security confgiuration is wrong. A guest could not reach "
                        + "the authentication site")
            );
    }
    
    /**
     * Tests if the authentication would return an Unauthorized status code if the request doesn't provide credentials.
     * 
     * @throws Exception
     */
    @Test
    public void guestAuthAccesabilityTest() throws Exception {
        this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                     .contentType(MediaType.TEXT_PLAIN)
                     .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test for {@link AuthController#authenticate(String, String)} 
     * (currently realized with {@link JwtAuthenticationFilter}). <br>
     * Real authentication test with a given password and username. Tests if return status code is 200 (OK). <br><br>
     * 
     * In order to run this tests, the credentials must have set in application-test.properties.
     * 
     * @throws Exception
     */
    @Test
    public void inMemoryAuthenticationTest() throws Exception {
        assumeTrue(inMemoryEnabled != null, "Could not load application-test.properties");
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "InMemory authentication must "
                + "be enabled in application-test.properties");
        assumeFalse(inMemoryPassword == null || inMemoryPassword.isBlank(), "Recovery password must be set in "
                + "application-test.properties");
        assumeFalse(inMemoryUser == null || inMemoryEnabled.isBlank(), "Recovery user must be set in"
                + " application-test.properties");
        this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                    .param("password", inMemoryPassword)
                    .param("username", inMemoryUser)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk());
    }
    
    /**
     * Tests if the server response send a JWT authorization token with the assumption of a successful 
     * authentication request. This could happen if the server authenticates the user successfully and return HTTP
     * status code 200 but does not generate a JWT token, does not set this token in the response header. 
     * 
     * @throws Exception
     */
    @Test
    public void jwtAuthenticationTest() throws Exception {
        assumeTrue(inMemoryEnabled != null, "Could not load application-test.properties");
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "InMemory authentication must "
                + "be enabled in application-test.properties");
        assumeFalse(inMemoryPassword == null || inMemoryPassword.isBlank(), "Recovery password must be set in "
                + "application-test.properties");
        assumeFalse(inMemoryUser == null || inMemoryEnabled.isBlank(), "Recovery user must be set in"
                + " application-test.properties");
        var result = this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                    .param("password", inMemoryPassword)
                    .param("username", inMemoryUser)
                    .accept(MediaType.TEXT_PLAIN))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is "
                + "another problem.");
        assumeTrue(jwtTokenHeader != null && jwtTokenPrefix != null, "You must set jwt.token.header and jwt.token.prefix in "
                + "application-test.properties in oder to run this test.");
        String partialHeader = result.getResponse().getHeader(jwtTokenHeader);
        assertAll(
               () -> assertNotNull(partialHeader, "No jwt token was returned during authentication"),
               () -> assertTrue(partialHeader.startsWith(jwtTokenPrefix), "Tokenheader does not start with the desired "
                       + "prefix."),
               () -> assertFalse(partialHeader.endsWith(jwtTokenPrefix), "The token header was found but no content "
                       + "which could be a JWT token")
           );

    }
    
}
