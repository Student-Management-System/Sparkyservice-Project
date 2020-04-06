package net.ssehub.sparkyservice.api.user;


import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;

class LocalUserDetailsTests {
    
    private PasswordEncoder encoder = new BCryptPasswordEncoder(); 
    
    @Test
    public void userDetailsFactoryPasswordTest() {
        final String password = "tst34";
        var userDetails = LocalUserDetails.newLocalUser("test", password, UserRole.DEFAULT);

        String passwordAlgo = notNull(userDetails.getPasswordEntity()).getHashAlgorithm();
        assertEquals(LocalUserDetails.DEFAULT_ALGO, passwordAlgo, 
                "Local password should be encoded with spring bcrypt");
        
        boolean passwordMatch = encoder.matches(password, userDetails.getPassword());
        assertTrue(passwordMatch, "The password was not correctly hashed with bcrypt in user factory method");
    }
    
    @Test
    public void userDetailsFactoryActiveTest() {
        var userDetails = LocalUserDetails.newLocalUser("test", "", UserRole.DEFAULT);
        assertTrue(userDetails.isActive());
    }
    
    @Test
    public void userDetailsFactoryDefaultRealmTest() {
        var userDetails = LocalUserDetails.newLocalUser("test", "", UserRole.DEFAULT);
        assertEquals(LocalUserDetails.DEFAULT_REALM, userDetails.getRealm(), 
                "The user is user details are not stored in the default realm.");
    }
}
