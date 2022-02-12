package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.auth.AuthController;
import net.ssehub.sparkyservice.api.auth.Identity;
import net.ssehub.sparkyservice.api.auth.JwtAuthenticationFilter;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Tests the whole authentication and authorization (with JWT tokens)
 * mechanisms with all kind of users.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // clears database
//checkstyle: stop exception type check
public class AuthenticationSecurityRestIT {

    @Autowired
    private UserStorageService userService; 

    @Autowired
    private WebApplicationContext context;
    @Value("${recovery.enabled}")
    private String inMemoryEnabled;
    @Value("${recovery.password}")
    private String inMemoryPassword;
    @Value("${recovery.user}")
    private String inMemoryUser;
    @Value("${jwt.header}")
    private String jwtTokenHeader;
    @Value("${jwt.prefix}")
    private String jwtTokenPrefix;
    private MockMvc mvc;

    /**
     * Setup is run before each tests and initialize the web context for mocking.
     */
    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
          .webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }

    /**
     * Test for {@link AuthController#authenticate(String, String)}. <br>
     * Tests the security configuration if the authentication site is accessible for guests. It ignores other errors
     * like Internal Server Errors (the whole 5XX Status group). 
     * 
     * @throws Exception
     */
    @IntegrationTest
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
    @IntegrationTest
    public void guestAuthAccesabilityTest() throws Exception {
        this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                     .contentType(MediaType.TEXT_PLAIN)
                     .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Tests if the authentication return unauthorized on wrong password.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void negativeAuthenticationTest() throws Exception {
        this.mvc.perform(
                post(ConfigurationValues.AUTH_LOGIN_URL)
                   .param("password", "sasd")
                   .param("username", "user")
                   .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isUnauthorized());
    }
    
    /**
     * Test for {@link AuthController#authenticate(String, String)} 
     * (currently realized with {@link JwtAuthenticationFilter}). <br>
     * Real authentication test with a given password and username. Tests if return status code is 200 (OK). <br><br>
     * 
     * In order to run this tests, the credentials must have set in test.properties.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void inMemoryAuthenticationTest() throws Exception {
        assumeTrue(inMemoryEnabled != null, "Could not load test.properties");
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "InMemory authentication must "
                + "be enabled in test.properties");
        assumeFalse(inMemoryPassword == null || inMemoryPassword.isBlank(), "Recovery password must be set in "
                + "test.properties");
        assumeFalse(inMemoryUser == null || inMemoryEnabled.isBlank(), "Recovery user must be set in"
                + " test.properties");
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
     * status code 200 but does not generate a JWT token or does not set this token in the response header. <br>
     * <br>
     * Uses an the inMemoryAuthentication account.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthenticationTest() throws Exception {
        assumeTrue(inMemoryEnabled != null, "Could not load test.properties");
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "InMemory authentication must "
                + "be enabled in test.properties");
        assumeFalse(inMemoryPassword == null || inMemoryPassword.isBlank(), "Recovery password must be set in "
                + "test.properties");
        assumeFalse(inMemoryUser == null || inMemoryEnabled.isBlank(), "Recovery user must be set in"
                + " test.properties");
        
        var result = this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                    .param("password", inMemoryPassword)
                    .param("username", inMemoryUser)
                    .accept(MediaType.TEXT_PLAIN))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is "
                + "another problem.");
        assumeTrue(jwtTokenHeader != null && jwtTokenPrefix != null, "You must set jwt.token.header and "
                + "jwt.token.prefix in test.properties in oder to run this test.");
        String partialHeader = result.getResponse().getHeader(jwtTokenHeader);
        assertAll(
            () -> assertNotNull(partialHeader, "No jwt token was returned during authentication"),
            () -> assertTrue(partialHeader.startsWith(jwtTokenPrefix), "Tokenheader does not start with the desired "
                 + "prefix."),
            () -> assertFalse(partialHeader.endsWith(jwtTokenPrefix), "The token header was found but no content "
                 + "which could be a JWT token")
        );

    }
    
    /**
     * Tests if an expired user is *not* able to authenticate.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void authenticationExpireTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("testuser", "password", UserRole.DEFAULT);
        user.setExpireDate(LocalDate.now().minusDays(1)); // user is expired
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        
        assumeTrue(inMemoryPassword != null && inMemoryEnabled.equals("true"));
        this.mvc
                .perform(
                     post(ConfigurationValues.AUTH_LOGIN_URL)
                        .param("password", "password")
                        .param("username", "testuser")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    /**
     * LDAP authentication test. After a successful authentication, a profile of the LDAP user should be stored into 
     * the database.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void storeUserAfterLdapAuthTest() throws Exception {
        var result = this.mvc
                .perform(
                     post(ConfigurationValues.AUTH_LOGIN_URL)
                        .param("password", "password")
                        .param("username", "gauss")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is "
                    + "another problem.");
        assertNotNull(userService.findUser(new Identity("gauss", UserRealm.LDAP)), 
                "User was not stored into " + UserRealm.LDAP + " realm.");
    }
    
    /**
     * Test for {@link AuthController#isTokenValid(org.springframework.security.core.Authentication)}.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void securityCheckAuthNegativeTest() throws Exception {
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());
    }
    
    /**
     * Tests an authentication attempt with a real JWT token from a real user (not with a mocked user).
     * Authentication should be successful.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthMemoryUser() throws Exception {
        assumeTrue(inMemoryPassword != null && inMemoryEnabled.equals("true"));
        var result = this.mvc
                .perform(
                     post(ConfigurationValues.AUTH_LOGIN_URL)
                        .param("password", inMemoryPassword)
                        .param("username", inMemoryUser)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        assertTrue(result.getResponse().getStatus() == 200, "Authentication not successful");
        String tokenHeader = result.getResponse().getHeader(jwtTokenHeader);
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .header(HttpHeaders.AUTHORIZATION, tokenHeader)
                   .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }

    /**
     * Tests an authentication attempt with a real JWT token and not with a mocked user.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthLocalUserTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("testuser", "password", UserRole.DEFAULT);
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        
        assumeTrue(inMemoryPassword != null && inMemoryEnabled.equals("true"));
        var result = this.mvc
                .perform(
                     post(ConfigurationValues.AUTH_LOGIN_URL)
                        .param("password", "password")
                        .param("username", "testuser")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        assertTrue(result.getResponse().getStatus() == 200, "Authentication not successful");
        var tokenHeader = result.getResponse().getHeader(jwtTokenHeader);
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .header(HttpHeaders.AUTHORIZATION, tokenHeader)
                   .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }

    /**
     * Tests if an authentication is possible through a configured ldap. 
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthLdapUserTest() throws Exception {
        var result = this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                    .param("password", "password")
                    .param("username", "gauss")
                    .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is"
                    + "another problem.");
        var tokenHeader = result.getResponse().getHeader(jwtTokenHeader);
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .header(HttpHeaders.AUTHORIZATION, tokenHeader)
                   .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }

    /**
     * Tests an authentication attempt with in memory username as password where the data is send as http content
     * in JSON format. 
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthJsonSerializeTest() throws Exception {
        //assumeTrue(inMemoryPassword != null && inMemoryEnabled.equals("true"));
        final String jsonCredentials = 
                "{ \"username\": \"" + inMemoryUser + "\" , \"password\": \"" + inMemoryPassword + "\"}";
        this.mvc
                .perform(
                     post(ConfigurationValues.AUTH_LOGIN_URL)
                     .content(jsonCredentials)
                     .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Testing the {@link ControllerPath#AUTHENTICATION_VERIFY} controller. 
     * <br>
     * Caution: We can't mock a user here. We need to make a real authentication and use the returned JWT token.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtVerifyTest() throws Exception {
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "Test can't be done wihtout memory credentials");
        var dto = new CredentialsDto();
        dto.password = inMemoryPassword;
        dto.username = inMemoryUser;
        ObjectMapper mapper = new ObjectMapper();
        String credentials = mapper.writeValueAsString(dto);
        var authResult = this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                    .content(credentials)
                    .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        assumeTrue(authResult.getResponse().getStatus() == 200, "Authentication with JWT Token was not successful");
        
        var tokenHeader = authResult.getResponse().getHeader(jwtTokenHeader).replace(jwtTokenPrefix, "");
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_VERIFY)
                   .param("jwtToken", tokenHeader)
                   .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }
}
