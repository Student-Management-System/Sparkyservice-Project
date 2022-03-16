package net.ssehub.sparkyservice.api.integration.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import net.ssehub.sparkyservice.api.auth.AuthenticationService;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@SpringBootTest
@AutoConfigureTestDatabase(replace=Replace.AUTO_CONFIGURED)
@ActiveProfiles("noldap")
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

}
