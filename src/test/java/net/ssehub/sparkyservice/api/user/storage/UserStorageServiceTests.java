package net.ssehub.sparkyservice.api.user.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;

/**
 * Tests for {@link UserStorageService} implementation. This test class should use the same implementation bean which 
 * is normally used in the application to make the test useful. 
 *
 * @author Marcel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
public class UserStorageServiceTests {
    
    @Autowired
    private UserStorageService userService;

    @MockBean
    private UserRepository mockedRepository;
    
    private static final String USER_NAME = "test213";
    private static final String USER_PW = "abcdefh";
    private static final UserRealm USER_REALM = LocalUserDetails.DEFAULT_REALM;

    private Optional<List<User>> userList;
    private Optional<User> user;

    @BeforeEach
    public void _setup() {
        var user1 = LocalUserDetails.newLocalUser(USER_NAME, USER_PW, UserRole.DEFAULT);
        var user2 = LocalUserDetails.newLocalUser(USER_NAME, USER_PW, UserRole.DEFAULT);
        user2.setRealm(UserRealm.MEMORY); // To simulate a working database, user with the same name should be in different realms
        user1.setId(1);
        user2.setId(2);
        this.userList = Optional.of(Arrays.asList(user1, user2));
        this.user = Optional.of(user1);
    }

    @Test
    public void findUserByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(userList);
        List<User> users = userService.findUsersByUsername(USER_NAME);
        assertTrue(!users.isEmpty(), "No user was loaded from service class");
    }

    @Test
    public void findMultipleUsersByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(userList);
        assertEquals(2, userService.findUsersByUsername(USER_NAME).size());
    }

    @Test
    public void findUserByNameAndRealmTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.user);
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertNotNull(loadedUser, "User was null.");
    }

    @Test
    public void findUserByNameAndRealmNullTest() throws UserNotFoundException {
        assertThrows(UserNotFoundException.class, 
                () -> userService.findUserByNameAndRealm(null, null));
    }

    @Test
    public void userNameValueTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.user);
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertEquals(USER_NAME, loadedUser.getUserName(), "Wrong username provided by user service");
    }

    @Test
    public void userRealmValueTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.user);
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertEquals(USER_REALM, loadedUser.getRealm(), "Wrong realm provided by user service");
    }
    
    /**
     * Test for {@link UserStorageImpl#userExistsInDatabase(User)}.
     * 
     * @throws UserNotFoundException
     */
    @Test
    public void userExistTest() throws UserNotFoundException {
        when(mockedRepository.findById(1)).thenReturn(user);
        user.ifPresent(u -> {
            assertTrue(userService.isUserInStorage(u));
        });
    }
    
    @Test
    public void userExistWithoutIdTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(user);
        user.ifPresent(u -> {
            assertTrue(userService.isUserInStorage(u));
        });
    }
    
    @Test
    public void userExistWithoutIdNegativeTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(Optional.empty());
        user.ifPresent(u -> {
            assertFalse(userService.isUserInStorage(u));
        });
    }
    
    @Test
    public void userExistNullTest() throws UserNotFoundException {
        user.ifPresent(u -> {
            assertFalse(userService.isUserInStorage(null));
        });
    }
    
    @Test
    public void loadUserByNameNegativeTest() {
        when(mockedRepository.findByuserNameAndRealm("djshfdjkhs", USER_REALM)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("djshfdjkhs"));
    }
    
    @Test
    public void loadUserByNameTest() {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(user);
        assertNotNull(userService.loadUserByUsername(USER_NAME));
    }

    @Test
    public void loadUserByNameNullTest() {
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(null));
    }

    @Test
    public void storeUserBlankTest() {
        var user = new User("", null, UserRealm.UNKNOWN, false, UserRole.DEFAULT);
        assertThrows(IllegalArgumentException.class, 
                () -> userService.commit(user));
    }

    @Test
    public void iterableToListCastTest() {
        when(mockedRepository.findByRealm(USER_REALM)).thenReturn(Arrays.asList(user.get()));
        var castedUserList = userService.findAllUsersInRealm(USER_REALM);
        assertAll(
                () -> assertEquals(1, castedUserList.size()),
                () -> assertEquals(user.get(), castedUserList.get(0))
            );
    }

    @Test
    public void nullKeepAliveDeleteTest() {
        assertDoesNotThrow(() -> userService.deleteUser(null));
    }

    @Test
    public void addUserTest() {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(Optional.ofNullable(null));
    }
}

