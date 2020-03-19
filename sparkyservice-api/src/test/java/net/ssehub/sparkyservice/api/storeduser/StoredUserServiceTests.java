package net.ssehub.sparkyservice.api.storeduser;

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

import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.db.user.StoredUser;

/**
 * Tests for {@link StoredUserService} logic. It will mock the repository to be sure only correct object will be 
 * returned. 
 * 
 *
 * @author Marcel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
public class StoredUserServiceTests {
    
    @Autowired
    private IStoredUserService userService;

    @MockBean
    private StoredUserRepository mockedRepository;
    
    private static final String USER_NAME = "test213";
    private static final String USER_PW = "abcdefh";
    private static final String USER_REALM = StoredUserDetails.DEFAULT_REALM;

    private Optional<List<StoredUser>> userList;
    private Optional<StoredUser> user;

    @BeforeEach
    public void _setup() {
        var user1 = StoredUserDetails.createStoredLocalUser(USER_NAME, USER_PW, true);
        var user2 = StoredUserDetails.createStoredLocalUser(USER_NAME, USER_PW, true);
        user2.setRealm("OTHER"); // To simulate a working database, user with the same name should be in different realms
        user1.setId(1);
        user2.setId(2);
        this.userList = Optional.of(Arrays.asList(user1, user2));
        this.user = Optional.of(user1);
    }

    @Test
    public void findUserByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(userList);
        List<StoredUserDetails> users = userService.findUsersByUsername(USER_NAME);
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
    public void findUserByNameAndRealmNegativeTest() throws UserNotFoundException {
        assertThrows(UserNotFoundException.class, 
                () -> userService.findUserByNameAndRealm(USER_NAME, "nonExistentRealm"));
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
     * Test for {@link StoredUserService#userExistsInDatabase(StoredUser)}.
     * 
     * @throws UserNotFoundException
     */
    @Test
    public void userExistTest() throws UserNotFoundException {
        when(mockedRepository.findById(1)).thenReturn(user);
        user.ifPresent(u -> {
            assertTrue(userService.isUserInDatabase(u));
        });
    }
    
    @Test
    public void userExistWithoutIdTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(user);
        user.ifPresent(u -> {
            assertTrue(userService.isUserInDatabase(u));
        });
    }
    
    @Test
    public void userExistWithoutIdNegativeTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(Optional.empty());
        user.ifPresent(u -> {
            assertFalse(userService.isUserInDatabase(u));
        });
    }
    
    @Test
    public void userExistNullTest() throws UserNotFoundException {
        user.ifPresent(u -> {
            assertFalse(userService.isUserInDatabase(null));
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
}

