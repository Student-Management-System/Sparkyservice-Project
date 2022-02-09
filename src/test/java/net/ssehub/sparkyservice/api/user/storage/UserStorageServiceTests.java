package net.ssehub.sparkyservice.api.user.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.Identity;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

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
    private static final @Nonnull UserRealm USER_REALM = UserRealm.LOCAL;
    private static final Identity IDENT = new Identity(USER_NAME, USER_REALM);

    private Optional<List<User>> jpaUserList;
    private Optional<User> jpaUser;
    private SparkyUser user;

    @BeforeEach
    public void _setup() {
        var user1 = LocalUserDetails.newLocalUser(USER_NAME, USER_PW, UserRole.DEFAULT);
        var user1Jpa = user1.getJpa();
        user1Jpa.setId(1);
        user1 = (LocalUserDetails) USER_REALM.getUserFactory().create(user1Jpa);
        this.user = user1;
        
        // To simulate a working database, user with the same name should be in different realms
        var user2 = UserRealm.LDAP.getUserFactory().create(USER_NAME, null, UserRole.DEFAULT, false);
        var user2Jpa = user2.getJpa();
        user2Jpa.setId(2);
        
        this.jpaUserList = Optional.of(Arrays.asList(user1Jpa, user2Jpa));
        this.jpaUser = Optional.of(user1Jpa);
    }

    @Test
    public void findUserByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(jpaUserList);
        List<SparkyUser> users = userService.findUsers(USER_NAME);
        assertTrue(!users.isEmpty(), "No user was loaded from service class");
    }

    @Test
    public void findMultipleUsersByNameTest() throws UserNotFoundException {
        when(mockedRepository.findByuserName(USER_NAME)).thenReturn(jpaUserList);
        assertEquals(2, userService.findUsers(USER_NAME).size());
    }

    @Test
    public void findUserByNameAndRealmNullTest() throws UserNotFoundException {
        assertThrows(UserNotFoundException.class, 
                () -> userService.findUser((Identity) null));
    }

    @Test
    public void userRealmValueTest() throws UserNotFoundException {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.jpaUser);
        var loadedUser = userService.findUser(IDENT);
        assertEquals(IDENT, loadedUser.getIdentity(), "Wrong user was loaded");
    }
    
    @DisplayName("Search for user with username as identifier positive test")
    @Test
    public void findUserWithUsernameTest() {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, USER_REALM)).thenReturn(this.jpaUser);
        assertDoesNotThrow(() -> userService.findUser(IDENT.asUsername()));
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
                USER_REALM.getUserFactory().create(jpaUserList.get().get(0));
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

