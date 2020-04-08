package net.ssehub.sparkyservice.api.integration.routing;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;

/**
 * For this test class, a set of routes was defined in the given properties file. 
 * All routes points to {@link ControllerPath#HEARTBEAT}.
 * 
 * @author Marcel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:test-routing.properties"})
public class RoutingIT {

    private static final String FREE_ROUTE = "/testroutefree";
    private static final String PROTECTED_ROUTE = "/testroutesecure";
    private static final String PROTECTED_LIST_ROUTE = "/testroutesecure2";

    @Autowired
    private JwtSettings jwtConf; 

    @Value("${recovery.enabled}")
    private boolean inMemoryEnabled;

    @Value("${recovery.password}")
    private String inMemoryPassword;

    @Value("${recovery.user}")
    private String inMemoryUser;    

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
          .webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
        assertTrue(inMemoryEnabled, "Memory authentication must be enabled for this test class - change it in "
                + "test-routing.properties");
    }

    /**
     * Test if a non authenticated user is forwarded when the path is not protected.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void guestNonProtectedRouteTest() throws Exception {
        this.mvc
            .perform(
                post(FREE_ROUTE)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().is2xxSuccessful());
    }

    /**
     * Tests if a guest can't access a protected route without authentication.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void guestProtectedRouteTest() throws Exception {
        this.mvc
            .perform(
                post(PROTECTED_ROUTE)
                .accept(MediaType.ALL))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Tests if an authorized user which can access the path.
     *  
     * @throws Exception
     */
    @IntegrationTest
    public void authorizedProtectedRouteTest() throws Exception {
        var roleList = Arrays.asList(UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtTokenWithRealm("user", roleList, jwtConf, UserRealm.MEMORY);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_ROUTE)
                   .header(HttpHeaders.AUTHORIZATION, fullTokenHeader)
                   .accept(MediaType.ALL))
           .andExpect(status().is2xxSuccessful());
    }

    /**
     * Tests if an authenticated user which is not authorized can't access the path. 
     *      
     * @throws Exception
     */
    @IntegrationTest
    public void authenticatedProtectedRouteTest() throws Exception {
        var roleList = Arrays.asList(UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtTokenWithRealm("anyUser", roleList, jwtConf, UserRealm.MEMORY);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_ROUTE)
                   .header(jwtConf.getHeader(), fullTokenHeader)
                   .accept(MediaType.ALL))
           .andExpect(status().isForbidden());
    }

    /**
     * Tests if an authenticated user which is not authorized can't access the path. The authorized configuration has a
     * set of users. All of these users must access the path in order to complete this test.
     *      
     * @throws Exception
     */
    @IntegrationTest
    public void authorizedListProtectedRouteTest() throws Exception {
        var roleList = Arrays.asList(UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtTokenWithRealm("user1", roleList, jwtConf, UserRealm.MEMORY);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(jwtConf.getHeader(), fullTokenHeader)
                   .accept(MediaType.ALL))
           .andExpect(status().is2xxSuccessful());
        
        jwtToken = JwtAuth.createJwtTokenWithRealm("user2", roleList, jwtConf, UserRealm.MEMORY);
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(jwtConf.getHeader(), fullTokenHeader)
                   .accept(MediaType.ALL))
           .andExpect(status().is2xxSuccessful());

        jwtToken = JwtAuth.createJwtTokenWithRealm("user3", roleList, jwtConf, UserRealm.MEMORY);
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(jwtConf.getHeader(), fullTokenHeader)
                   .accept(MediaType.ALL))
           .andExpect(status().is4xxClientError());
    }

}
