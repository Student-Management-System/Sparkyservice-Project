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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.testconf.RealmBeanConfiguration;
import net.ssehub.sparkyservice.api.testconf.TestSetupMethods;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LdapRealm;
import net.ssehub.sparkyservice.api.user.LocalRealm;
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
@ContextConfiguration(classes= {RealmBeanConfiguration.class})
public class UserStorageServiceTests {
    
    private UserStorageService userService;

    @MockBean
    private UserRepository mockedRepository;
    
    private static final String USER_NAME = "test213";
    private static final String USER_PW = "abcdefh";
    
    private UserRealm localRealm = new LocalRealm();
    private Identity ident;
    private Optional<List<User>> jpaUserList;
    private Optional<User> jpaUser;
    private SparkyUser user;

    @SuppressWarnings("null")
    @BeforeEach
    public void _setup() {
        var ldapRealm = new LdapRealm();
        this.ident = new Identity(USER_NAME, localRealm);
        TestSetupMethods.testRealmSetup(ldapRealm, localRealm);
        this.userService = new UserDatabaseStorageService(mockedRepository);
        
        var user1 = LocalUserDetails.newLocalUser(USER_NAME, localRealm, USER_PW, UserRole.DEFAULT);
        var user1Jpa = user1.getJpa();
        user1Jpa.setId(1);
        user1 = (LocalUserDetails) localRealm.userFactory().create(user1Jpa);
        this.user = user1;
        
        // To simulate a working database, user with the same name should be in different realms
        var user2 = ldapRealm.userFactory().create(USER_NAME, null, UserRole.DEFAULT, false);
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
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, localRealm.identifierName())).thenReturn(this.jpaUser);
        var loadedUser = userService.findUser(ident);
        assertEquals(ident, loadedUser.getIdentity(), "Wrong user was loaded");
    }
    
    @DisplayName("Search for user with username as identifier positive test")
    @Test
    public void findUserWithUsernameTest() {
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, localRealm.identifierName())).thenReturn(this.jpaUser);
        var n = ident.asUsername();
        assertDoesNotThrow(() -> userService.findUser(ident.asUsername()));
    }
    
    /**
     * Test for {@link UserDatabaseStorageService#userExistsInDatabase(User)}.
     * findUserByNameAndRealm
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
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, localRealm.identifierName())).thenReturn(jpaUser);
        assertTrue(userService.isUserInStorage(user));
    }
    
    @Test
    public void userExistWithoutIdNegativeTest() throws UserNotFoundException {
        when(mockedRepository.findById(0)).thenReturn(Optional.empty());
        when(mockedRepository.findByuserNameAndRealm(USER_NAME, localRealm.identifierName())).thenReturn(Optional.empty());
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
        when(mockedRepository.findByRealm(localRealm.identifierName())).thenReturn(Arrays.asList(jpaUser.get()));
        var userList = userService.findAllUsersInRealm(localRealm);
        @SuppressWarnings("null") var castedUser = 
                localRealm.userFactory().create(jpaUserList.get().get(0));
        assertAll(
            () -> assertEquals(1, userList.size()),
            () -> assertTrue(user.equals(castedUser))
        );
    }

    @Test
    @Disabled //TODO enable this again
    public void nullKeepAliveDeleteTest() {
        assertDoesNotThrow(() -> userService.deleteUser(null));
    }

}

