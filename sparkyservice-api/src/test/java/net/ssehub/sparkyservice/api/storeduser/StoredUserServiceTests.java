package net.ssehub.sparkyservice.api.storeduser;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        this.userList = Optional.of(Arrays.asList(user1, user2));
        this.user = Optional.of(user1);
        
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.user);
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(userList);
    }

    @Test
    public void findUserByNameTest() throws UserNotFoundException {
        List<StoredUserDetails> users = userService.findUsersByUsername(USER_NAME);
        assertTrue(!users.isEmpty(), "No user was loaded from service class");
    }

    @Test
    public void findMultipleUsersByNameTest() throws UserNotFoundException {
        assertEquals(2, userService.findUsersByUsername(USER_NAME).size());
    }

    @Test
    public void findUserByNameAndRealmTest() throws UserNotFoundException {
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
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertEquals(USER_NAME, loadedUser.getUserName(), "Wrong username provided by user service");
    }

    @Test
    public void userRealmValueTest() throws UserNotFoundException {
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertEquals(USER_REALM, loadedUser.getRealm(), "Wrong realm provided by user service");
    }
}

