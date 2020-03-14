package net.ssehub.sparkyservice.api.storeduser.dto;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.storeduser.MissingDataException;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto;
import net.ssehub.sparkyservice.api.storeduser.dto.SettingsDto;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto.ChangePasswordDto;

@SuppressWarnings("null")
public class EditUserDtoTests {
    
    private StoredUserDetails user;
    private static final String newPassword = "testPassword";
    private static final String oldPassword = "oldPw123";
    private static final String userEmaiL = "info@test";
    
    @BeforeEach
    public void access() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Constructor<StoredUserDetails> constructor = StoredUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        user = constructor.newInstance();
    }
    
    /**
     * Creates a simple and complete {@link EditUserDto} object for testing purposes. 
     * 
     * @return complete testing dto
     */
    public EditUserDto createExampleDto() {
        var editUserDto = new EditUserDto();
        editUserDto.username = "user";
        editUserDto.realm = "realm";
        editUserDto.passwordDto = new ChangePasswordDto();
        editUserDto.passwordDto.newPassword = newPassword;
        editUserDto.passwordDto.oldPassword = oldPassword;
        editUserDto.settings = new SettingsDto();
        editUserDto.settings.email_address = userEmaiL;
        editUserDto.settings.email_receive = true;
        editUserDto.settings.wantsAi = true;
        return editUserDto;
    }
    
   
    /**
     * Test for {@link EditUserDto#changePasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is correctly changed inside user object.
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changePasswordDtoTest() throws MissingDataException {
        user.encodeAndSetPassword(oldPassword);
        var userDto = createExampleDto();
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        EditUserDto.changePasswordFromDto(user, userDto.passwordDto);
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }
    
    /**
     * Test for {@link EditUserDto#changePasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is unchanged if the passwordDto provides the wrong old password. 
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changeWrongPasswordDtoTest() throws MissingDataException {
        var passwordDto = new ChangePasswordDto();
        passwordDto.oldPassword = "abcdef";
        passwordDto.newPassword = newPassword;
        user.encodeAndSetPassword(oldPassword);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        EditUserDto.changePasswordFromDto(user, passwordDto);
        assertFalse(encoder.matches(newPassword, user.getPassword()));
    }
    
    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     *
     * @throws MissingDataException
     */
    @Test
    public void editUserPasswordFromDtoTest() throws MissingDataException {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        user.encodeAndSetPassword(oldPassword);
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }
    
    
    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * The given User is in the local realm and has no password entitiy. In reality this should never happen, but when
     * it happen a {@link RuntimeException} should be thrown to indicate that something went wrong.
     */
    @Test
    public void editUserPasswordFromDtoNegativeTest() {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        assertThrows(RuntimeException.class, () -> EditUserDto.editUserFromDtoValues(user, createExampleDto()));
    }
    
    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editUserEmailFromDtoTest() throws MissingDataException {
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        assertEquals(userEmaiL, user.getProfileConfiguration().getEmail_address(), "User email was not changed in user "
                + "object");
    }
    
    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editUserNameFromDtoTest() throws MissingDataException {
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        assertEquals("user", user.getUsername(), "Username was not changed in user "
                + "object");
    }
    
//    /**
//     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
//     * {@link #createExampleDto()}
//     */
//    @Test
//    @Disabled("Test if changing the realm is supported (currently there is no requirement for it")
//    public void editUserRealmFromDtoTest() throws MissingDataException {
//        EditUserDto.editUserFromDtoValues(user, createExampleDto());
//        assertEquals("realm", user.getRealm());
//    }
}
