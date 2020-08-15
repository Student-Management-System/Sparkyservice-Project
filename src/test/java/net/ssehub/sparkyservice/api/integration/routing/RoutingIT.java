package net.ssehub.sparkyservice.api.integration.routing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;

/**
 * For this test class, a set of routes was defined in the given properties file. 
 * All routes points to {@link ControllerPath#HEARTBEAT}.
 * 
 * @author Marcel
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:test-routing.properties"})
@AutoConfigureMockMvc
//checkstyle: stop exception type check
public class RoutingIT {

    public static final String PROTECTED_LIST_ROUTE = "/testroutesecure2";
    private static final String FREE_ROUTE = "/testroutefree";
    private static final String PROTECTED_ROUTE = "/testroutesecure/heartbeat";

    @Autowired
    private JwtSettings jwtConf; 

    @Autowired
    private MockMvc mvc;

    /**
     * Test if a non authenticated user is forwarded when the path is not protected.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void guestNonProtectedRouteTest() throws Exception {
        this.mvc
            .perform(
                get(FREE_ROUTE)
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
                get(PROTECTED_ROUTE)
                .accept(MediaType.ALL))
            .andExpect(status().isForbidden());
    }

    /**
     * Tests if an authorized user which can access the path.
     *  
     * @throws Exception
     */
    @IntegrationTest
    public void authorizedProtectedRouteTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("user", "", UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtToken(user, jwtConf);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_ROUTE)
                   .header(HttpHeaders.PROXY_AUTHORIZATION, fullTokenHeader)
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
        var user = LocalUserDetails.newLocalUser("anyUser", "", UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtToken(user, jwtConf);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().isForbidden());
    }

    /**
     * Tests if an authenticated user which is not authorized for the requested resource can't access it. 
     * The ACL has a set of users and all of these users must access the path in order to complete this test.
     *      
     * @throws Exception
     */
    @IntegrationTest
    public void authorizedListProtectedRouteTest() throws Exception {
        var user1 = LocalUserDetails.newLocalUser("user1", "", UserRole.DEFAULT);
        var user2 = LocalUserDetails.newLocalUser("user2", "", UserRole.DEFAULT);
        String jwtToken = JwtAuth.createJwtToken(user1, jwtConf);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());
        
        jwtToken = JwtAuth.createJwtToken(user2, jwtConf);
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());

        // Negative test to check if this mechanism still work even with a list.
        var user3 = LocalUserDetails.newLocalUser("user3", "", UserRole.DEFAULT);
        jwtToken = JwtAuth.createJwtToken(user3, jwtConf); 
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is4xxClientError());
    }
}
