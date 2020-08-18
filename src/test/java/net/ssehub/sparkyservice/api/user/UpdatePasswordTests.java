package net.ssehub.sparkyservice.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;;

/**
 * Test class for {@link SparkyUser#updatePassword(ChangePasswordDto, UserRole)}.
 * 
 * @author marcel
 */
public class UpdatePasswordTests {

    /**
     * Test if password updates for Memory users being ignored.
     */
    @Test
    public void memoryUserPasswordTest() {
        var user = new MemoryUser("test", new Password("test", "plain"), UserRole.ADMIN);
        var pwDto = new ChangePasswordDto();
        pwDto.newPassword = "yes";
        pwDto.oldPassword = "test";
        user.updatePassword(pwDto, UserRole.ADMIN);
        
        assertEquals(user.getPassword(), "test", "The password for memory user shouldn't be changed"); 
    }

    /**
     * Test if password updates for LDAP user result in an error when queried.
     */
    @Test
    public void ldapUserPasswordTest() {
        var user = new LdapUser("test", UserRole.ADMIN, true);
        var pwDto = new ChangePasswordDto();
        user.updatePassword(pwDto, UserRole.ADMIN);

        assertThrows(UnsupportedOperationException.class, () -> user.getPassword(), "LDAP user never have passwords"); 
    }

    /**
     * Test if a password could succesfully changed when invoked with {@link UserRole#DEFAULT} permissions with a 
     * correct provided old password.
     */
    @Test
    public void localUserPasswordPositivTest() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(); // must be the same encoder as used in user
        @SuppressWarnings("null") var pw = new Password(
            encoder.encode("test"), BCryptPasswordEncoder.class.getSimpleName()
        );
        var user = new LocalUserDetails("test", pw, false, UserRole.ADMIN);
        var pwDto = new ChangePasswordDto();
        pwDto.newPassword = "yes";
        pwDto.oldPassword = "test";
        user.updatePassword(pwDto, UserRole.DEFAULT);
        
        assumeTrue(user.getPasswordEntity().getHashAlgorithm().equalsIgnoreCase(pw.getHashAlgorithm()));
        assertTrue(encoder.matches("yes", user.getPassword()), "The password be changed to a new one changed"); 
    }
}
