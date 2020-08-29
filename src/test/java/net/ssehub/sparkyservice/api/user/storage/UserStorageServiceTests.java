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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.creation.UserFactoryProvider;

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

    private Optional<List<User>> jpaUserList;
    private Optional<User> jpaUser;
    private SparkyUser user;

    @BeforeEach
    public void _setup() {
        var user1 = LocalUserDetails.newLocalUser(USER_NAME, USER_PW, UserRole.DEFAULT);
        var user1Jpa = user1.getJpa();
        user1Jpa.setId(1);
        user1 = (LocalUserDetails) UserFactoryProvider.getFactory(USER_REALM).create(user1Jpa);
        this.user = user1;
        
        // To simulate a working database, user with the same name should be in different realms
        var user2 = UserFactoryProvider.getFactory(UserRealm.LDAP).create(USER_NAME, null, UserRole.DEFAULT, false);
        var user2Jpa = user2.getJpa();
        user2Jpa.setId(2);
        
        this.jpaUserList = Optional.of(Arrays.asList(user1Jpa, user2Jpa));
        this.jpaUser = Optional.of(user1Jpa);
    }

    @Test
    public void findUserByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(jpaUserList);
        List<SparkyUser> users = userService.findUsersByUsername(USER_NAME);
        assertTrue(!users.isEmpty(), "No user was loaded from service class");
    }

    @Test
    public void findMultipleUsersByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(jpaUserList);
        assertEquals(2, userService.findUsersByUsername(USER_NAME).size());
    }

    @Test
    public void findUserByNameAndRealmTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.jpaUser);
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
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.jpaUser);
        var loadedUser = userService.findUserByNameAndRealm(USER_NAME, USER_REALM);
        assertEquals(USER_NAME, loadedUser.getUsername(), "Wrong username provided by user service");
    }

    @Test
    public void userRealmValueTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.jpaUser);
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
        when(mockedRepository.findById(1)).thenReturn(jpaUser);
        assertTrue(userService.isUserInStorage(user));
    }
    
    @Test
    public void userExistWithoutIdTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(jpaUser);
        assertTrue(userService.isUserInStorage(user));
    }
    
    @Test
    public void userExistWithoutIdNegativeTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(Optional.empty());
        assertFalse(userService.isUserInStorage(user));
    }
    
    @Test
    public void userExistNullTest() throws UserNotFoundException {
        jpaUser.ifPresent(u -> {
            assertFalse(userService.isUserInStorage(null));
        });
    }

    @Test
    public void findAllUserInRealmTypeTest() {
        when(mockedRepository.findByRealm(USER_REALM)).thenReturn(Arrays.asList(jpaUser.get()));
        var userList = userService.findAllUsersInRealm(USER_REALM);
        @SuppressWarnings("null") var castedUser = 
                UserFactoryProvider.getFactory(USER_REALM).create(jpaUserList.get().get(0));
        assertAll(
            () -> assertEquals(1, userList.size()),
            () -> assertTrue(user.equals(castedUser))
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

