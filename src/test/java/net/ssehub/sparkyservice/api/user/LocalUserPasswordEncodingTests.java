package net.ssehub.sparkyservice.api.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.testconf.DummyRealm;

public class LocalUserPasswordEncodingTests {
    
    private static final UserRealm REALM = new DummyRealm("dummy");

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
        var user = LocalUserDetails.newLocalUser("testname", REALM, passwordToCheck, UserRole.DEFAULT);
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue(match, "Encoded password does not match with bcrypted password");
    }

    @ParameterizedTest
    @MethodSource("data")
    public void manualPasswordHashMethodTest(@Nonnull String passwordToCheck) {
        var user = LocalUserDetails.newLocalUser("testname", REALM, passwordToCheck, UserRole.DEFAULT);
        user.encodeAndSetPassword(passwordToCheck);
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        assertTrue(match);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void negativeManualPasswordHashMethodTest(@Nonnull String passwordToCheck) {
        var user = LocalUserDetails.newLocalUser("testname", REALM, passwordToCheck, UserRole.DEFAULT);
        user.encodeAndSetPassword(passwordToCheck + "1");
        boolean match = encoder.matches(passwordToCheck, user.getPassword());
        if (passwordToCheck.length() > 71) {// bcrypt max 
            assertTrue(match, "BCrypt max length is reached.");
        } else {
            assertFalse(match, "The password should be wrong, but is assumed as correct");
        }
    }
    // maybe check setPassword() with plain text
}
