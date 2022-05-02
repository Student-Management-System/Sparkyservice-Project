package net.ssehub.sparkyservice.api.auth.jwt;

import static java.time.LocalDateTime.now;
import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.testconf.TestSetupMethods;
import net.ssehub.sparkyservice.api.user.LdapRealm;
import net.ssehub.sparkyservice.api.user.LocalRealm;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Provides unit tests for {@link JwtTokenService}.
 * 
 * @author marcel
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {JwtTestStorageBeanConf.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class JwtTokenServiceTests {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtAuthReader reader;
    
    @Autowired
    private JwtStorageService jwtStorageService;
    
    @Autowired 
    private UserStorageService userStorageService;
    
    @Nonnull
    private final SparkyUser testUser;

    public JwtTokenServiceTests() {
        var ldapRealm = new LdapRealm();
        TestSetupMethods.testRealmSetup(ldapRealm, new LocalRealm());
        testUser = ldapRealm.userFactory().create("testUser", null, UserRole.ADMIN, true);
    }

    @SuppressWarnings("null")
    @BeforeEach
    public void setupJwtService() {
        userStorageService.commit(testUser);
        assertTrue(jwtStorageService != null, "Test setup failed");
        JwtCache.initNewCache(new HashSet<JwtToken>(), jwtStorageService);
    }
    
    @Test
    @DisplayName("Disabling JWT by JIT test")
    public void disableJwtTest() throws JwtTokenReadException {
        var testUser = userStorageService.findUser(this.testUser.getUsername());
        String jwtString = jwtTokenService.createFor(testUser);
        JwtToken tokenObj = reader.readJwtToken(jwtString);
        JwtCache.getInstance().refreshFromStorage();
        jwtTokenService.disable(tokenObj.getJti());
        assertAll(
            () -> assertFalse(tokenObj.isLocked(), "New token shouldn't be locked from start"),
            () -> assertFalse(jwtTokenService.isJitNonLocked(tokenObj.getJti()), "Token is not locked in storage")
        );
    }

    @DisplayName("Token locked state matches with state of the token")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void isTokenNonLockedTest(boolean isLocked) {
        var tokenObj = new JwtToken(notNull(UUID.randomUUID()), notNull(now()), testUser.getUsername(), UserRole.ADMIN);
        tokenObj.setLocked(isLocked);
        jwtStorageService.commit(tokenObj);
        JwtCache.getInstance().refreshFromStorage();
        assertEquals(isLocked, !jwtTokenService.isJitNonLocked(tokenObj.getJti()));
    }

    @Test
    @DisplayName("Token is not logged when not in database test")
    public void isTokenNonLoggedNonExistingTest() { 
        var tokenObj = new JwtToken(notNull(UUID.randomUUID()), notNull(now()), testUser.getUsername(), UserRole.ADMIN);
        assertTrue(jwtTokenService.isJitNonLocked(tokenObj.getJti()));
    }
    
    @Test
    @DisplayName("Disabling list of JWT by JIT test")
    @SuppressWarnings("null")
    public void disableJwtListTest() throws JwtTokenReadException {
        var jitArray = new UUID[5];
        
        for (int i = 0; i < 5; i++) {
            String jwtString = jwtTokenService.createFor(testUser);
            jitArray[i] = reader.readJwtToken(jwtString).getJti();
        }
        jwtTokenService.disable(jitArray);
        for (var jit : jitArray) {
            assertFalse(jwtTokenService.isJitNonLocked(jit));
        }
    }

    @Test
    @DisplayName("Disable all JWT from a user test")
    public void disableJwtTokenFromUserTest() throws JwtTokenReadException {
        var tokenArray = new JwtToken[5];
        var testUserDb = userStorageService.refresh(testUser);
        for (int i = 0; i < 5; i++) {
            String jwtString = jwtTokenService.createFor(testUserDb);
            tokenArray[i] = reader.readJwtToken(jwtString);
        }
        jwtTokenService.disableAllFrom(testUserDb);
        for (JwtToken jwt : tokenArray) {
            JwtToken tokenInCache = JwtCache.getInstance().getCachedToken(jwt.getJti()).get();
            assertTrue(tokenInCache.isLocked(), "JWT of testUser is not locked in cache");
        }
    }

}
