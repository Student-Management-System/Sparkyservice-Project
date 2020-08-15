package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.auth.JwtAuthorizationFilter;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.conf.SpringConfig;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.storage.ServiceAccStorageService;
import net.ssehub.sparkyservice.api.user.storage.TestingUserRepository;

/**
 * Tests if locked users can't authorize. The locked user list is only loaded during spring boots startup, so a list of
 * locked service accounts is injected during a TestConfiguration class {@link TestDatabaseValues}. 
 * <br><br>
 * Class tests (1) the correct creation of the locked JWT list in {@link SpringConfig#lockedJwtToken()} and <br>
 * (2) if the authorization process do not allow those tokens {@link JwtAuthorizationFilter}.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:test-routing.properties"})
@Import(LockedAccountsAuthorizeIT.TestDatabaseValues.class)
@AutoConfigureMockMvc
//checkstyle: stop exception type check
public class LockedAccountsAuthorizeIT {
    
    private static final String USERNAME = "service"; // "service" must be allowed in test.properties for PROTECTED_PATH

    private static final String PROTECTED_PATH = "/testroutesecure2";

    @Autowired
    private JwtSettings jwtConf; 

    @Autowired
    private MockMvc mvc;

    @Autowired
    @Qualifier("testUser")
    private User testUser;

    @Autowired
    @Qualifier(SpringConfig.LOCKED_JWT_BEAN)
    private Set<String> lockedJwt;

    /**
     * Test configuration class for early bean modification.
     * 
     * @author marcel
     */
    @TestConfiguration
    static class TestDatabaseValues {

        @Autowired
        private JwtSettings jwtConf; 

        @MockBean
        private TestingUserRepository mockedRepository;

        @Autowired
        @Qualifier("testUser")
        private User testUser;

        /**
         * Override the default implementation in order to mock the repository and return a locked service account.
         * This class is used in {@link SpringConfig#lockedJwtToken()} to get a list of SERVICE accounts. 
         * 
         * Creates an service account and set a token as payload in order to lock it 
         * 
         * @return Default Service
         */
        @Bean
        @Primary
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public ServiceAccStorageService earlyService() {
            System.out.println("CALL EXT");
            var set = new HashSet<User>();
            set.add(testUser);
            when(mockedRepository.findByRole(UserRole.SERVICE)).thenReturn(set);
            return new ServiceAccStorageService();
        }

        /**
         * Bean definition for test cases. A user is prepared for test cases.
         * 
         * @return Returns the test user which holds a locked JWT token as payload.
         */
        @Bean("testUser")
        public User testUser() {
            var user = LocalUserDetails.newLocalUser(USERNAME, "test", UserRole.SERVICE);
            String jwtToken = JwtAuth.createJwtToken(user, jwtConf);
            user.getProfileConfiguration().setPayload(jwtToken);
            return user;
        }
        
    }
    
    /**
     * Tests a route where the token of the service locked (available in the payload).
     * The account authorized himself with the same token from the payload which should be locked.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void routingJwtLockedTest() throws Exception {
        String jwtToken = testUser.getProfileConfiguration().getPayload();
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        
        assertFalse(lockedJwt.isEmpty(), "Testsetup is wrong. No token is locked");
        this.mvc
            .perform(
                get(PROTECTED_PATH)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().isForbidden());
    }

    /**
     * Tries to authorize a protected route with a valid account and a non locked token (other tokens may be locked 
     * from the current account).
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void routingJwtNonLockedTest() throws Exception {
        String jwtToken = JwtAuth.createJwtToken(testUser, jwtConf);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_PATH)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());
    }

    /**
     * Tests if a locked JWT of a service account can't authorize normally.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void nonAuthorizeLockedJwtTest() throws Exception {
        String jwtToken = testUser.getProfileConfiguration().getPayload();
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .header(HttpHeaders.AUTHORIZATION, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().isForbidden());
    }
}
