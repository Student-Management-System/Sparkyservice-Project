package net.ssehub.sparkyservice.api.user;


import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.storage.DuplicateEntryException;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageImpl;
import net.ssehub.sparkyservice.api.user.storage.UserStorageServiceTests;

/**
 * Test class for storing information into a database with an in-memory database with {@link UserStorageImpl}.
 * The logic checks will be done in {@link UserStorageServiceTests} where the repositories are mocked and will 
 * return correct objects. 
 * 
 * @author Marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class StorageServiceImplDatabaseTests {
    
    @Autowired
    private UserStorageImpl storageService;
    
    private static final String TEST_USER_NAME = "eatk234";
    
    @BeforeEach
    public void _storeUserToDB() {
        @SuppressWarnings("deprecation") var user = new LocalUserDetails();
        user.setActive(true);
        user.setRealm(LocalUserDetails.DEFAULT_REALM);
        user.setUserName(TEST_USER_NAME);
        user.setRole(UserRole.DEFAULT);
        storageService.commit(user);
    }

    /**
     * Positive test for storing a {@link LocalUserDetails} into the database. 
     * 
     * @throws UserNotFoundException if user is not found in database
     */
    @Test
    public void storeUserDetailsTest() throws UserNotFoundException {
        User loadedUser = storageService.findUserById(1);
        assertAll(
                () -> assertEquals(TEST_USER_NAME, loadedUser.getUserName()),
                () -> assertEquals(LocalUserDetails.DEFAULT_REALM, loadedUser.getRealm()),
                () -> assertEquals(UserRole.DEFAULT, loadedUser.getRole()),
                () -> assertTrue(loadedUser.isActive())
            );
    }

    @Test
    public void storedUserDetailsInstanceTest() throws UserNotFoundException {
        var storedUser = storageService.findUserByNameAndRealm(TEST_USER_NAME, LocalUserDetails.DEFAULT_REALM);
        assertTrue(storedUser instanceof LocalUserDetails, "Users in the local realm should be an instance "
                + "of " + LocalUserDetails.class.getName());
    }

    /**
     * Positive test for finding a user by name and realm. 
     * 
     * @throws UserNotFoundException if user is not found in database
     */
    @Test
    public void findUserTest() throws UserNotFoundException {
        User loadedUser = storageService.findUserByNameAndRealm(TEST_USER_NAME, LocalUserDetails.DEFAULT_REALM);
        assertNotNull(loadedUser, "User was not loaded from database.");
    }
    
    @Test
    public void changeRoleValueAndStoreTest() throws UserNotFoundException {
        User loadedUser = storageService.findUserByNameAndRealm(TEST_USER_NAME, LocalUserDetails.DEFAULT_REALM);
        loadedUser.setRole(UserRole.ADMIN);
        storageService.commit(loadedUser);
        loadedUser = storageService.findUserByNameAndRealm(TEST_USER_NAME, LocalUserDetails.DEFAULT_REALM);
        assertEquals(UserRole.ADMIN, loadedUser.getRole(), "The role was not changed inside the datbase.");
    }
    
    @Test
    public void dataDuplicateUserTest() {
        @SuppressWarnings("deprecation") var secondUser = new LocalUserDetails();
        secondUser.setUserName(TEST_USER_NAME);
        secondUser.setRealm(LocalUserDetails.DEFAULT_REALM);
        assertThrows(DataIntegrityViolationException.class, () -> storageService.commit(secondUser));
    }
    
    /**
     * Test if the application function is guaranteed when searching for null 
     * 
     * @throws UserNotFoundException
     */
    @Test
    public void findUserNullTest() throws UserNotFoundException {
        assertThrows(UserNotFoundException.class, () -> storageService.findUserByNameAndRealm(null, null), "Null is not"
                + " supported as input while finding users."); 
    }

    @Test
    public void findMultipleEntries() throws UserNotFoundException {
        @SuppressWarnings("deprecation") var user = new LocalUserDetails();
        user.setActive(true);
        user.setRealm(UserRealm.LDAP);
        user.setUserName(TEST_USER_NAME);
        storageService.commit(user);
        var users = storageService.findUsersByUsername(TEST_USER_NAME);
        assertEquals(2, users.size());
    }

    /**
     * Tests if a username can be found and has the right type.
     */
    @Test
    public void loadByUserNameInstanceTest() {
        assertTrue(storageService.loadUserByUsername(TEST_USER_NAME) instanceof LocalUserDetails);
    }

    /**
     * Tests if an {@link UsernameNotFoundException} is thrown when spring loads his user details.
     */
    @Test
    public void loadMissingNameTest() {
        assertThrows(UsernameNotFoundException.class, () -> storageService.loadUserByUsername("notExisting"));
    }

    /**
     * Tests that multiple commits on the same user does not throw an exception. 
     */
    @Test
    public void commitUserTwiceTest() {
        var user = storageService.findUserByNameAndRealm(TEST_USER_NAME, UserRealm.LOCAL);
        assertDoesNotThrow(() -> storageService.commit(user));
    }

    /**
     * When a new user is added twice, it should throw an exception. 
     */
    @Test
    public void addUserTest() {
        assertThrows(DuplicateEntryException.class, () -> storageService.addUser(TEST_USER_NAME));
    }

    /**
     * Test if a new user is added with correct values.
     */
    @Test
    public void addUserValuesTest() {
        LocalUserDetails newUser = storageService.addUser("name");
        assertAll(
            () -> assertEquals(newUser.getRealm(), UserRealm.LOCAL, "User is in the wrong realm"),
            () -> assertEquals(newUser.getUsername(), "name", "User with wrong name was created")
        );
    }
}
