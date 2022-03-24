package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import net.ssehub.sparkyservice.api.auth.SetAuthenticationFilter;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Provides tests for JWTs locking and refreshing mechanisms. 
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = {"classpath:test-routing.properties"})
//checkstyle: stop exception type check
public class LockedJwtStatucCodeIT {

    // "service@local" must be allowed in test-routing.properties for PROTECTED_PATH
    private static final String USERNAME = "service"; 
    private static final String PROTECTED_PATH = "/testroutesecure2";
    @Nonnull
    private static final SparkyUser TEST_USER = LocalUserDetails.newLocalUser(USERNAME, "test", UserRole.SERVICE);
    
    @Autowired private JwtSettings jwtConf; 
    @Autowired private JwtTokenService jwtService; 
    @Autowired private MockMvc mvc;
    private String jwtString;
    
    @MockBean
    private UserStorageService userStorageService;
    
    /*
     * Hotfix:
     * Not used in this test class. When running tests in this class when other integrations tests run before, 
     * spring have problems with autowiren the configuration properties.
     */
    @Autowired
    private ZuulRoutes zuulRoutes;
    
    @BeforeEach
    public void setupJwtCache() {
        JwtCache.initNewCache();
        assertNotNull(zuulRoutes.getRoutes());
        var user = LocalUserDetails.newLocalUser(USERNAME, "test", UserRole.SERVICE);
        
        this.jwtString = jwtService.createFor(user);
        Set<JwtToken> lockedToken = JwtCache.getInstance().getCachedTokens();
        lockedToken.forEach(t -> t.setLocked(true));
        JwtCache.initNewCache(lockedToken, null);
        assertFalse(JwtCache.getInstance().getLockedJits().isEmpty(), "Cache with locked JWT shouldn't be empty");
    }

    /**
     * Tests if a locked JWT of a service account can't authorize normally via {@link SetAuthenticationFilter}.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DisplayName("Test if request is not routed with locked JWT")
    public void routingJwtLockedTest() throws Exception {
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtString;
        this.mvc
            .perform(
                get(PROTECTED_PATH)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().isUnauthorized());
    }


    /**
     * Tries to authorize a protected route with a valid account and a non locked token (other tokens may be locked 
     * from the current account).
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DisplayName("Test request is proxied with NON locked JWT")
    public void routingNonLockedJwtTest() throws Exception {
        String newJwtToken = jwtService.createFor(TEST_USER);
        String fullTokenHeader = jwtConf.getPrefix() + " " + newJwtToken;
        this.mvc
            .perform(
                get(PROTECTED_PATH)
                   .header(ZuulAuthorizationFilter.PROXY_AUTH_HEADER, fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().is2xxSuccessful());
    }

    /**
     * Tests if the user can't authorize via authorization filter when his token is locked.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @DisplayName("Test if user can't authorize with locked JWT")
    public void authorizeLockedTest() throws Exception {
        String fullTokenHeader = jwtConf.getPrefix() + " " + jwtString;
        Mockito.when(userStorageService.findUser(TEST_USER.getIdentity())).thenReturn(TEST_USER);
        this.mvc
            .perform(
                get(ControllerPath.AUTHENTICATION_CHECK)
                   .header(jwtConf.getHeader(), fullTokenHeader)
                   .accept(MediaType.ALL))
            .andExpect(status().isUnauthorized());
    }
}
