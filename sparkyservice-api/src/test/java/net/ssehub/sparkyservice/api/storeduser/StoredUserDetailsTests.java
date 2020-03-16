package net.ssehub.sparkyservice.api.storeduser;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;;


class StoredUserDetailsTests {
    
    private PasswordEncoder encoder = new BCryptPasswordEncoder(); 
    
    @Test
    public void userDetailsFactoryPasswordTest() {
        final String password = "tst34";
        var userDetails = StoredUserDetails.createStoredLocalUser("test", password, false);

        String passwordAlgo = notNull(userDetails.getPasswordEntity()).getHashAlgorithm();
        assertEquals(StoredUserDetails.DEFAULT_ALGO, passwordAlgo, 
                "Local password should be encoded with spring bcrypt");
        
        boolean passwordMatch = encoder.matches(password, userDetails.getPassword());
        assertTrue(passwordMatch, "The password was not correctly hashed with bcrypt in user factory method");
    }
    
    @Test
    public void userDetailsFactoryActiveTest() {
        var userDetails = StoredUserDetails.createStoredLocalUser("test", "", false);
        assertFalse(userDetails.isActive());
    }
    
    @Test
    public void userDetailsFactoryDefaultRealmTest() {
        var userDetails = StoredUserDetails.createStoredLocalUser("test", "", false);
        assertEquals(StoredUserDetails.DEFAULT_REALM, userDetails.getRealm(), 
                "The user is user details are not stored in the default realm.");
    }
    
    @Test
    public void userSetRoleTest() {
        var userDetails = new StoredUserDetails();
        userDetails.setUserRole(UserRole.DEFAULT);
        assertEquals(UserRole.DEFAULT.name(), userDetails.getTransactionObject().getRole(), "Userrole was not translated "
                + "from ENUM type to string type (which is necessary in oder to store the object into the database.");
    }
}
