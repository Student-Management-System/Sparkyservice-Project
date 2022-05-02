package net.ssehub.sparkyservice.api.auth.local;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.testconf.DummyRealm;
import net.ssehub.sparkyservice.api.useraccess.UserRole;

class LocalUserDetailsTests {

    private PasswordEncoder encoder = new BCryptPasswordEncoder();

    private LocalUserDetails user;

    @BeforeEach
    void createUser() {
        user = LocalUserDetails.newLocalUser("test", new DummyRealm("dummy"), "", UserRole.DEFAULT);
    }
    
    @Test
    public void userDetailsFactoryPasswordTest() {
        final String password = "tst34";
        var userDetails = LocalUserDetails.newLocalUser("test", new DummyRealm(""), password, UserRole.DEFAULT);

        String passwordAlgo = notNull(userDetails.getPasswordEntity()).getHashAlgorithm();
        assertEquals(LocalUserDetails.DEFAULT_ALGO, passwordAlgo,
            "Local password should be encoded with spring bcrypt");

        boolean passwordMatch = encoder.matches(password, userDetails.getPassword());
        assertTrue(passwordMatch, "The password was not correctly hashed with bcrypt in user factory method");
    }

    @Test
    public void userDetailsFactoryActiveTest() {
        assertTrue(user.isEnabled());
    }

    @Test
    public void userDetailsFactoryDefaultRealmTest() {
        assertEquals("dummy", user.getIdentity().realm().identifierName(),
            "The user is user details was not stored in the provided realm.");
    }

    @Test
    public void userExpirationDefaultTest() {
        assertTrue(user.isAccountNonExpired(), "User shouldn't be expired by default");
    }

    @Test
    public void userExpirationFunctionTest() {
        user.setExpireDate(LocalDate.now().minusDays(1)); // is expired
        assertFalse(user.isAccountNonExpired(), "User should be expired!");
    }
}
