package net.ssehub.sparkyservice.api.user;


import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.user.LocalUserDetails;;


class LocalUserDetailsTests {
    
    private PasswordEncoder encoder = new BCryptPasswordEncoder(); 
    
    @Test
    public void userDetailsFactoryPasswordTest() {
        final String password = "tst34";
        var userDetails = LocalUserDetails.createStoredLocalUser("test", password, false);

        String passwordAlgo = notNull(userDetails.getPasswordEntity()).getHashAlgorithm();
        assertEquals(LocalUserDetails.DEFAULT_ALGO, passwordAlgo, 
                "Local password should be encoded with spring bcrypt");
        
        boolean passwordMatch = encoder.matches(password, userDetails.getPassword());
        assertTrue(passwordMatch, "The password was not correctly hashed with bcrypt in user factory method");
    }
    
    @Test
    public void userDetailsFactoryActiveTest() {
        var userDetails = LocalUserDetails.createStoredLocalUser("test", "", false);
        assertFalse(userDetails.isActive());
    }
    
    @Test
    public void userDetailsFactoryDefaultRealmTest() {
        var userDetails = LocalUserDetails.createStoredLocalUser("test", "", false);
        assertEquals(LocalUserDetails.DEFAULT_REALM, userDetails.getRealm(), 
                "The user is user details are not stored in the default realm.");
    }
}
