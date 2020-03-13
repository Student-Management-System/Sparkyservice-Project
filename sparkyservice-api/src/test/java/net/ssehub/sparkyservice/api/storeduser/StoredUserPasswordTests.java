package net.ssehub.sparkyservice.api.storeduser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class StoredUserPasswordTests {
      
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

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
    

    @ParameterizedTest
    @MethodSource("data")
    public void constructorPasswordHashTest(@Nonnull String passwordToCheck) {
        var user = StoredUserDetails.createStoredLocalUser("testname", passwordToCheck, true);
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue("Encoded password does not match with bcrypted password", match);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void manualPasswordHashMethodTest(@Nonnull String passwordToCheck) {
        var user = StoredUserDetails.createStoredLocalUser("testname", passwordToCheck, true);
        user.encodeAndSetPassword(passwordToCheck);
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue(match);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void negativeManualPasswordHashMethodTest(@Nonnull String passwordToCheck) {
        var user = StoredUserDetails.createStoredLocalUser("testname", passwordToCheck, true);
        user.encodeAndSetPassword(passwordToCheck + "1");
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        if (passwordToCheck.length() > 71) {// bcrypt max 
            assertTrue("BCrypt max length is reached.", match);
        } else {
            assertFalse("The password should be wrong, but is assumed as correct", match);
        }
    }
    // maybe check setPassword() with plain text
}
