package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
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

import net.ssehub.sparkyservice.api.auth.storage.JwtCache;
import net.ssehub.sparkyservice.api.auth.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.testconf.JwtTestBeanConf;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LdapUserFactory;
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
@ContextConfiguration(classes = {UnitTestDataConfiguration.class, JwtTestBeanConf.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class JwtTokenServiceTests {

    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtStorageService jwtStorageService;

    @Autowired 
    private UserStorageService userStorageService;
    
    @Nonnull
    private final SparkyUser testUser;

    public JwtTokenServiceTests() {
        testUser = new LdapUserFactory().create("testUser", null, UserRole.ADMIN, true);
    }

    @BeforeEach
    public void setupJwtService() {
        userStorageService.commit(testUser);
        assertTrue(jwtStorageService != null, "Test setup failed");
        JwtCache.initNewCache(new HashSet<JwtToken>(), jwtStorageService);
        jwtTokenService = new JwtTokenService(UnitTestDataConfiguration.sampleJwtConf());
    }
    
    @Test
    @DisplayName("Disabling JWT by JIT test")
    public void disableJwtTest() throws JwtTokenReadException {
        var testUser = notNull(
            userStorageService.findUsersByUsername(this.testUser.getUsername()).get(0)
        );
        String jwtString = jwtTokenService.createFor(testUser);
        JwtToken tokenObj = jwtTokenService.readJwtToken(jwtString);
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
        var userInfo = new AuthPrincipalImpl(testUser.getRealm(), testUser.getUsername());
        var tokenObj = new JwtToken(notNull(UUID.randomUUID()), 
                new Date(System.currentTimeMillis()), userInfo, UserRole.ADMIN);
        tokenObj.setLocked(isLocked);
        jwtStorageService.commit(tokenObj);
        JwtCache.getInstance().refreshFromStorage();
        assertEquals(isLocked, !jwtTokenService.isJitNonLocked(tokenObj.getJti()));
    }

    @Test
    @DisplayName("Token is not logged when not in database test")
    public void isTokenNonLoggedNonExistingTest() { 
        var tokenObj = new JwtToken(notNull(UUID.randomUUID()), 
                new Date(System.currentTimeMillis()), new AuthPrincipalImpl("LOCAL", "test"), UserRole.ADMIN);
        assertTrue(jwtTokenService.isJitNonLocked(tokenObj.getJti()));
    }
    
    @Test
    @DisplayName("Disabling list of JWT by JIT test")
    @SuppressWarnings("null")
    public void disableJwtListTest() throws JwtTokenReadException {
        var jitArray = new UUID[5];
        
        for (int i = 0; i < 5; i++) {
            String jwtString = jwtTokenService.createFor(testUser);
            jitArray[i] = jwtTokenService.readJwtToken(jwtString).getJti();
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
            tokenArray[i] = jwtTokenService.readJwtToken(jwtString);
        }
        jwtTokenService.disableAllFrom(testUserDb);
        for (JwtToken jwt : tokenArray) {
            JwtToken tokenInCache = JwtCache.getInstance().getCachedToken(jwt.getJti()).get();
            assertTrue(tokenInCache.isLocked(), "JWT of testUser is not locked in cache");
        }
    }

}
