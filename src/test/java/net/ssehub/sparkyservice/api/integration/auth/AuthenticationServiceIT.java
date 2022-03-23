package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    @Disabled("Disabled for CI")
    @DisplayName("Test if ldap user login is possible")
    public void loginLdapTest() {
        var creds = new CredentialsDto();
        var ident = new Identity("test", UserRealm.UNIHI);
        creds.username = ident.asUsername();
        creds.password = "secret";
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

}
