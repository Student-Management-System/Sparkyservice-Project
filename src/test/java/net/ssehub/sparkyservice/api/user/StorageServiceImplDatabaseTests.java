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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.jpa.user.Password;
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

    private static final Identity TEST_USER = new Identity("eatk234", LocalUserDetails.DEFAULT_REALM);
    
    @BeforeEach
    public void _storeUserToDB() {
        var user = new LocalUserFactory().create(TEST_USER.nickname(), new Password("test", "PLAIN"), UserRole.DEFAULT, true);
        user.setEnabled(true);
        user.setRole(UserRole.DEFAULT);
        user.encodeAndSetPassword("test");
        storageService.commit(user);
    }

    /**
     * Positive test for storing a {@link LocalUserDetails} into the database. 
     * 
     * @throws UserNotFoundException if user is not found in database
     */
    @Test
    public void storeUserDetailsTest() throws UserNotFoundException {
        SparkyUser loadedUser = storageService.findUser(1);
        assertAll(
                () -> assertEquals(TEST_USER, loadedUser.getIdentity()),
                () -> assertEquals(UserRole.DEFAULT, loadedUser.getRole()),
                () -> assertTrue(loadedUser.isEnabled(), "User should be enabled in database")
            );
    }

    @Test
    public void storedUserDetailsInstanceTest() throws UserNotFoundException {
        var storedUser = storageService.findUser(TEST_USER);
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
        SparkyUser loadedUser = storageService.findUser(TEST_USER);
        assertNotNull(loadedUser, "SparkyUser was not loaded from database.");
    }
    
    @Test
    public void findUserViaUsernameTest() {
        assertDoesNotThrow(() -> storageService.findUser(TEST_USER.asUsername()));
    }
    
    @Test
    public void changeRoleValueAndStoreTest() throws UserNotFoundException {
        SparkyUser loadedUser = storageService.findUser(TEST_USER);
        loadedUser.setRole(UserRole.ADMIN);
        storageService.commit(loadedUser);
        loadedUser = storageService.findUser(TEST_USER);
        assertEquals(UserRole.ADMIN, loadedUser.getRole(), "The role was not changed inside the datbase.");
    }
    
    @Test
    public void dataDuplicateUserTest() {
        var secondUser = LocalUserDetails.newLocalUser(TEST_USER.nickname(), "", UserRole.DEFAULT);
        assertThrows(DataIntegrityViolationException.class, () -> storageService.commit(secondUser));
    }
    
    /**
     * Test if the application function is guaranteed when searching for empty name 
     * 
     * @throws UserNotFoundException
     */
    @Test
    public void findUserEmptyNameTest() throws UserNotFoundException {
        assertThrows(IllegalArgumentException.class, () -> storageService.findUser(""), "Empty username is not"
                + " supported as input for finding users."); 
    }

    @Test
    public void findMultipleEntriesUsingOnlyNickname() throws UserNotFoundException {
        var user = UserRealm.UNIHI.getUserFactory().create(TEST_USER.nickname(), null, UserRole.DEFAULT, true);
        storageService.commit(user);
        var users = storageService.findUsers(TEST_USER.nickname());
        assertEquals(2, users.size());
    }


    /**
     * Tests that multiple commits on the same user does not throw an exception. 
     */
    @Test
    public void commitUserTwiceTest() {
        var user = storageService.findUser(TEST_USER);
        assertDoesNotThrow(() -> storageService.commit(user));
    }

    /**
     * When a new user is added twice, it should throw an exception. 
     */
    @Test
    public void addUserTest() {
        assertThrows(DuplicateEntryException.class, () -> storageService.addUser(TEST_USER.nickname()));
    }

    /**
     * Test if a new user is added with correct values.
     */
    @Test
    public void addUserValuesTest() {
        LocalUserDetails newUser = storageService.addUser("name");
        assertAll(
            () -> assertEquals(newUser.getIdentity().realm(), UserRealm.LOCAL, "SparkyUser is in the wrong realm"),
            () -> assertEquals(newUser.getIdentity().nickname(), "name", "SparkyUser with wrong name was created")
        );
    }
}
