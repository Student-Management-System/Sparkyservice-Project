package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.auth.AuthenticationService;
import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@SpringBootTest
@AutoConfigureTestDatabase(replace=Replace.NONE)
@DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
@Transactional
@ActiveProfiles("test")
public class AuthenticationServiceIT {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private UserStorageService userService;

    @Value("${recovery.password}")
    private String inMemoryPassword;

    @Value("${recovery.user}")
    private String inMemoryUser;

    @Test
    @DisplayName("Test if only the correct realm is used when specified")
    @SuppressWarnings("null")
    public void authDuplicatesCorrectRealm() throws Exception {
        // create duplicate nicknames in different realms
        var user = LocalUserDetails.newLocalUser(inMemoryUser, inMemoryPassword, UserRole.DEFAULT);
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        var localUserIdent = new Identity(inMemoryUser, LocalUserDetails.DEFAULT_REALM);
        var creds = new CredentialsDto();
        creds.username = localUserIdent.asUsername();
        creds.password = inMemoryPassword;
        Authentication authResult = authService.authenticate(creds);
        assumeTrue(authResult.isAuthenticated());
        assumeTrue(authResult.getPrincipal() instanceof SparkyUser);
        
        var authenticatedUser = (SparkyUser) authResult.getPrincipal();
        assertTrue(authenticatedUser.getIdentity().equals(user.getIdentity()), 
                "The user which was authenticated was not the requested one");
    }

    @Test
    @DisplayName("Test authentication method of memory user")
    public void memoryUserAuthenticationTest() {
        var creds = new CredentialsDto();
        creds.username = inMemoryUser;
        creds.password = inMemoryPassword;

        var resultAuth = authService.authenticate(creds);
        assertTrue(resultAuth.isAuthenticated());
    }

    @Test
    @DisplayName("Test authentication method of memory user with realm information")
    @SuppressWarnings("null")
    public void memoryUserAuthenticationRealmTest() {
        var creds = new CredentialsDto();
        creds.username = new Identity(inMemoryUser, UserRealm.RECOVERY).asUsername();
        creds.password = inMemoryPassword;
        var resultAuth = authService.authenticate(creds);
        assertTrue(resultAuth.isAuthenticated());
    }
    
    @IntegrationTest
    @DisplayName("Test if ldap user login is possible")
    public void loginLdapTest() {
        // default credentials from: https://www.forumsys.com/2014/02/22/online-ldap-test-server/
        var ident = new Identity("gauss", UserRealm.UNIHI);
        var creds = new CredentialsDto(ident.asUsername(), "password"); 
        assertDoesNotThrow(() -> authService.authenticate(creds));
    }
    
    @IntegrationTest
    @DisplayName("Test if auth attempt with bad credentials fails with correct exception")
    public void negativeLoginTest() {
        var creds = new CredentialsDto();
        creds.username = "aaaaaaaa";
        creds.password = "";
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(creds));
    }
    
    @IntegrationTest
    @DisplayName("Test if unkown realm auth attempt is denied properly")
    public void negativeUnkownRealmTest() {
        var creds = new CredentialsDto();
        creds.username = "aaaaaaaa@asdasd";
        creds.password = "asdasd";
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(creds));
    }
    
    @IntegrationTest
    @DisplayName("Test if realm is added to username when missing")
    public void addRealmAuthTest() {
        assumeFalse(Identity.validateFormat(inMemoryPassword));
        var creds = new CredentialsDto(inMemoryUser, inMemoryPassword);
        var auth = authService.authenticate(creds);
        assumeTrue(auth.isAuthenticated());
        assertTrue(Identity.validateFormat(auth.getName()));
    }
    
    
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private UserStorageService service;
    @IntegrationTest
    @DisplayName("Test if login with locked jwt are denied")
    public void lockedJwtDeniedTest() {
        JwtCache.initNewCache();
        var user = LocalUserDetails.newLocalUser("testuser", "test", UserRole.SERVICE);
        String jwtString = jwtTokenService.createFor(user);
        Set<JwtToken> lockedToken = JwtCache.getInstance().getCachedTokens();
        lockedToken.forEach(t -> t.setLocked(true));
        JwtCache.initNewCache(lockedToken, null);
        service.commit(user);
        assumeFalse(JwtCache.getInstance().getLockedJits().isEmpty(), "Cache with locked JWT shouldn't be empty");
        
        HttpServletRequest  mockedRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockedRequest.getHeader("Authorization")).thenReturn("Bearer " + jwtString);
        assertThrows(AuthenticationException.class, () -> authService.checkAuthenticationStatus(mockedRequest));
    }

}
