package net.ssehub.sparkyservice.api.integration.routing;

import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * For this test class, a set of routes was defined in the given properties file. 
 * All routes points to {@link ControllerPath#HEARTBEAT}.
 * 
 * @author Marcel
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = {"classpath:test-routing.properties"})
//checkstyle: stop exception type check
public class RoutingIT {

    public static final String PROTECTED_LIST_ROUTE = "/testroutesecure2";
    private static final String FREE_ROUTE = "/testroutefree";
    private static final String PROTECTED_ROUTE = "/testroutesecure/heartbeat";
    private static final String EXPECTATION_HEADER_ROUTE = "/authvalidation";

    @Autowired
    private JwtSettings jwtConf; 

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private MockMvc mvc;
    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }
 
    @AfterAll
    public static void stopServer() { 
        mockServer.stop();
    }
    private static final String EXAMPLE_JWT = "Bearer abcdefgh";

    /**
     * Mocked http server with a listen route on /authvalidation on 127.0.0.1:1080 - maybe configure this route
     * in test-routing.properties to use it in this test class. <br>
     * When the expected route is called it will return an 302 otherwise an 404. 
     */
    private void createExpectationForValidAuthHeader() {
        new MockServerClient("127.0.0.1", 1080)
          .when(
            request()
              .withMethod("GET")
              .withPath("/authvalidation")
              .withHeader("\"Content-type\", \"application/json\"")
              .withHeader(HttpHeaders.AUTHORIZATION, EXAMPLE_JWT),
              exactly(1))
                .respond(
                  response()
                    .withStatusCode(302)
                    .withHeaders(
                      new Header("Content-Type", "application/json; charset=utf-8"),
                      new Header("Cache-Control", "public, max-age=86400"))
                    .withBody("{ message: 'incorrect authorization header }")
                    .withDelay(TimeUnit.SECONDS,1)
                );
    }

    /**
     * Tests if the authorization header is preserverd and forwared when using the routing filter.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @Disabled // when running alone, the test probably succeed. This happens through strange behaviour of zuul when 
    // running all tests together. Spring maybe autowires something wrong. The RequestContext is completly different 
    // To produce the error: Run all tests through maven(!)
    public void preserveAuthHeaderWhileRoutingTest() throws Exception {
        createExpectationForValidAuthHeader();
        mvc.perform(
            get(EXPECTATION_HEADER_ROUTE)
                .header(HttpHeaders.AUTHORIZATION,  EXAMPLE_JWT)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isFound());
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
            .andExpect(status().isUnauthorized());
    }

    /**
     * Tests if an authorized user which can access the path.
     *  
     * @throws Exception
     */
    @IntegrationTest
    public void authorizedProtectedRouteTest() throws Exception {
        var user = LocalUserDetails.newLocalUser("user", "", UserRole.DEFAULT);
        String jwtToken = jwtService.createFor(user);
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
        String jwtToken = jwtService.createFor(user);
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
    @DisplayName("Mutli user ACL on protected route test")
    public void authorizedListProtectedRouteTest() throws Exception {
        var user1 = LocalUserDetails.newLocalUser("user1", "", UserRole.DEFAULT);
        var user2 = LocalUserDetails.newLocalUser("user2", "", UserRole.DEFAULT);
        String jwtToken = jwtService.createFor(user1);
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());
        
        jwtToken = jwtService.createFor(user2);
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());

        // Negative test to check if this mechanism still work even with a list.
        var user3 = LocalUserDetails.newLocalUser("user3", "", UserRole.DEFAULT);
        jwtToken = jwtService.createFor(user3);
        fullTokenHeader = jwtConf.getPrefix() + " " + jwtToken;
        this.mvc
            .perform(
                get(PROTECTED_LIST_ROUTE)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is4xxClientError());
    }
}
