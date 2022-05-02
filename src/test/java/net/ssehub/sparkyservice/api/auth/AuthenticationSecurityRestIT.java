package net.ssehub.sparkyservice.api.auth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.ldap.LdapRealm;
import net.ssehub.sparkyservice.api.auth.local.LocalRealm;
import net.ssehub.sparkyservice.api.auth.local.LocalUserDetails;
import net.ssehub.sparkyservice.api.auth.memory.MemoryRealm;
import net.ssehub.sparkyservice.api.config.ConfigurationValues;
import net.ssehub.sparkyservice.api.config.ControllerPath;
import net.ssehub.sparkyservice.api.persistence.UserStorageService;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.dto.CredentialsDto;

/**
 * Tests the whole authentication and authorization (with JWT tokens)
 * mechanisms with all kind of users.
 * 
 * @author marcel
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace=Replace.ANY)
@ActiveProfiles("test")
@SuppressWarnings("null")
//checkstyle: stop exception type check
public class AuthenticationSecurityRestIT {

    @Autowired
    private UserStorageService userService; 
    @Autowired
    private LocalRealm localRealm;
    @Autowired
    private MemoryRealm memoryRealm;
    @Autowired(required = false)
    private LdapRealm ldapRealm;


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
    
    @Autowired
    private ObjectMapper objMapper;
    
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
     * Tests if the authentication would return an Unauthorized status code if the request doesn't provide credentials.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DisplayName("Test if user can access the login page. Expected bad request since no body is provided ")
    public void guestAuthAccesabilityTest() throws Exception {
        this.mvc
            .perform(
                 post(ConfigurationValues.AUTH_LOGIN_URL)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    /**
     * Tests if the authentication return unauthorized on wrong password.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void negativeAuthenticationTest() throws Exception {
        var req = createAuthenticationRequest("user", "wrongpw");
        mvc.perform(req).andExpect(status().isUnauthorized()).andDo(print());
    }
    
    /**
     * Test for {@link AuthController#authenticate(String, String)} 
     * (currently realized with {@link DoAuthenticationFilter}). <br>
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
        var req = createAuthenticationRequest(inMemoryUser, inMemoryPassword);
        mvc.perform(req).andExpect(status().isOk());
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
        
        var request = createAuthenticationRequest(inMemoryUser, inMemoryPassword);
        this.mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(containsString(jwtTokenPrefix)));
    }
    
    /**
     * Tests if an expired user is *not* able to authenticate.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DirtiesContext
    public void authenticationExpireTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("testuser", localRealm, "password", UserRole.DEFAULT);
        user.setExpireDate(LocalDate.now().minusDays(1)); // user is expired
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        
        assumeTrue(inMemoryPassword != null && inMemoryEnabled.equals("true"));
        var request = createAuthenticationRequest("testuser", "password");
        this.mvc.perform(request).andExpect(status().isUnauthorized());
    }
    /**
     * LDAP authentication test. After a successful authentication, a profile of the LDAP user should be stored into 
     * the database.
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    @IntegrationTest
    @Disabled("No test AD server available")
    public void storeUserAfterLdapAuthTest() throws Exception {
        var username = "";
        var result = mvcPeformLogin(username, "");
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is "
                    + "another problem.");
        assertNotNull(userService.findUser(new Identity(username, ldapRealm)), 
                "User was not stored into " + ldapRealm.identifierName() + " realm.");
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
            .andExpect(status().isUnauthorized());
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
        var result = mvcPeformLogin(inMemoryUser, inMemoryPassword);
        assertEquals(OK.value(), result.getResponse().getStatus(), "Authentication not successful");
        
        var jwt = readWhoamiResponse(result.getResponse()).jwt.token;
        assumeTrue(jwt != null, "No JWT token response");
        mvc.perform(createWhoamiRequest(jwt)).andExpect(status().isOk());
    }

    /**
     * Tests an authentication attempt with a real JWT token and not with a mocked user.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DirtiesContext
    public void jwtAuthLocalUserTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("testuser", localRealm, "password", UserRole.DEFAULT);
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        
        var result = mvcPeformLogin("testuser", "password");
        assertEquals(OK.value(), result.getResponse().getStatus(), "Authentication not successful");
        var jwt = readWhoamiResponse(result.getResponse()).jwt.token;
        assumeTrue(jwt != null, "No JWT token response");
        mvc.perform(createWhoamiRequest(jwt)).andExpect(status().isOk());
    }

    /**
     * Tests if an authentication is possible through a configured ldap. 
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void jwtAuthLdapUserTest() throws Exception {
        var result = mvcPeformLogin(inMemoryUser, inMemoryPassword);
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication was not successful - maybe there is"
                    + "another problem.");
        var jwt = readWhoamiResponse(result.getResponse()).jwt.token;
        mvc.perform(createWhoamiRequest(jwt)).andExpect(status().isOk());
    }
    
    @IntegrationTest
    @DisplayName("Test if authentication with JSON Dto is successful")
    public void authJsonTest() throws Exception {
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "Test can't be done wihtout memory credentials");
        mvc.perform(createAuthenticationRequest(inMemoryUser, inMemoryPassword))
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
    @DisplayName("Test if a memory users JWT can be verified through controller method")
    public void jwtVerifyTest() throws Exception {
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "Test can't be done wihtout memory credentials");
        var result = mvcPeformLogin(inMemoryUser, inMemoryPassword);
        assumeTrue(result.getResponse().getStatus() == 200, "Authentication with JWT Token was not successful");
        
        var jwt = readWhoamiResponse(result.getResponse()).jwt.token;
        mvc.perform(createJwtVerifyRequest(jwt)).andExpect(status().isOk());
    }
    
    @IntegrationTest
    @DisplayName("Test if auth with specified realm is successfull")
    public void authSpecifiedRealmTest() throws Exception {
        assumeTrue(Boolean.parseBoolean(inMemoryEnabled), "Test can't be done wihtout memory credentials");
        @SuppressWarnings("null")
        var ident = new Identity(inMemoryUser, memoryRealm);
        var resultCode = mvcPeformLogin(ident.asUsername(), inMemoryPassword).getResponse().getStatus();
        assertEquals(OK.value(), resultCode);
    }
    
    public String createCredentialsJson(String username, String password) throws JsonProcessingException {
        var dto = new CredentialsDto();
        dto.password = password;
        dto.username = username;
        return objMapper.writeValueAsString(dto);
    }
    
    public MockHttpServletRequestBuilder createJwtVerifyRequest(String tokenHeader) throws Exception {
        return get(ControllerPath.AUTHENTICATION_VERIFY)
           .param("jwtToken", tokenHeader)
           .accept(MediaType.APPLICATION_JSON_VALUE);
    }
    
    public MvcResult mvcPeformLogin(String username, String password) throws Exception {
        var request = createAuthenticationRequest(username, password);
        return this.mvc
            .perform(request)
            .andReturn();
    }

    private MockHttpServletRequestBuilder createAuthenticationRequest(String username, String password) throws JsonProcessingException {
        String jsonCredentials = createCredentialsJson(username, password);
        var request = post(ConfigurationValues.AUTH_LOGIN_URL)
                .content(jsonCredentials)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        return request;
    }
    
    public MockHttpServletRequestBuilder createWhoamiRequest(String tokenHeader) throws Exception {
        return get(ControllerPath.AUTHENTICATION_CHECK)
            .header(HttpHeaders.AUTHORIZATION, tokenHeader)
            .accept(MediaType.APPLICATION_JSON_VALUE);
    }
    
    public AuthenticationInfoDto readWhoamiResponse(MockHttpServletResponse response) throws JsonMappingException, JsonProcessingException, UnsupportedEncodingException {
        String content = response.getContentAsString();
        return objMapper.readValue(content, AuthenticationInfoDto.class);
    }
}
