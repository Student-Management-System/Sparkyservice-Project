package net.ssehub.sparkyservice.api.storeduser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RunWith(Parameterized.class)
public class StoredUserPasswordTests {
   
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final StoredUserDetails user;
    
    @Nonnull
    private String passwordToCheck;
    
    @Parameters(name = "{index}: {0}")
    public  static String[] data() {
        return new String [] {
                "342kjudsmnfbsjakhfjkhdfkfhejkrfhewakjhsdfkjfhewjkfhaskdjhjkdshklaewakfjiou324ioklujfdsklajfekljsfdkljf"
                + "jsakdlfjdsklfjsdaklfjadsklfjdsklfjsdlakjsdlkajfdslkjfkldsjfklsjriotzhhcjkghdfjkfdshfjkdfshgfjkhgsjkd"
                + "dkjfaklfjdsklfjsdklfjsdkalhruithjhfdgjkhjdekhgfdjkhgsfdjkhgfksjhgkjerzhtruhdfjkjweiorudkslfjghdkhjgk"
                + "djfgkhjkrdslfhgkjfdhgjkfdshgjkhreijhkjhsdakfjieowufdskjghfdjghfdskhjioterutrjhgkfdhieruidfjhgkjdhgjk"
                + "jkfdhgjksdhfgjkhdfkgjhfsdkjghruekztuigfdhgkjfdhsgjhreuhgfudhgkurthugrthsukgjhrjeskhguirhsgjkflshdkh", 
                "#ä+ä§$%$§%ÄÖ$Ä§ÖÄ$%Ö$§%\"\"\"", 
                "12345678912345678912345678913245678915645678912345678955512456789132456",
                " ",
                "a"};
    }
    
    public StoredUserPasswordTests(@Nonnull String passwordInput) {
        this.passwordToCheck = passwordInput;
        user = StoredUserDetails.createStoredLocalUser("testname", passwordInput, true);
    }
    
    @Test
    public void constructorPasswordHashTest() {
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue("Encoded password does not match with bcrypted password", match);
    }
    
    @Test
    public void manualPasswordHashMethodTest() {
        user.hashAndSetPassword(passwordToCheck);
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue(match);
    }
    
    @Test
    public void negativeManualPasswordHashMethodTest() {
        user.hashAndSetPassword(passwordToCheck + "1");
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        if (passwordToCheck.length() > 71) {// bcrypt max 
            assertTrue("BCrypt max length is reached.", match);
        } else {
            assertFalse("The password should be wrong, but is assumed as correct", match);
        }
    }
    // maybe check setPassword() with plain text
}
