package net.ssehub.sparkyservice.api.integration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.storeduser.IStoredUserService;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.StoredUserService;
import net.ssehub.sparkyservice.api.storeduser.UserNotFoundException;
import net.ssehub.sparkyservice.api.storeduser.UserRole;
import net.ssehub.sparkyservice.api.testing.UnitTestDataConfiguration;

/**
 * Integration test class for storing information into a database with
 * {@link StoredUserService}.
 * 
 * @author Marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
public class StoredUserServiceIT {
    
    @Autowired
    private IStoredUserService userService;

    private static final String TEST_USER_NAME = "eatk234";
    
    @BeforeEach
    public void _storeUserToDB() {
        var user = StoredUserDetails.createStoredLocalUser(TEST_USER_NAME, "", true);
        userService.storeUser(user);
    }
    
    /**
     * Positive test for storing a {@link StoredUserDetails} into the database. 
     * 
     * @throws UserNotFoundException if user is not found in database
     */
    @Test
    @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
    public void storeUserDetailsTest() throws UserNotFoundException {
        var loadedUser = userService.findUserByid(1);
        assertEquals(TEST_USER_NAME, loadedUser.getUserName());
        assertEquals(StoredUserDetails.DEFAULT_REALM, loadedUser.getRealm());
        assertEquals(UserRole.DEFAULT.name(), loadedUser.getRole());
        assertTrue(loadedUser.isActive());
    }
    
    /**
     * Positive test for finding a user by name and realm. 
     * 
     * @throws UserNotFoundException if user is not found in database
     */
    @Test
    @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
    public void storeAndFindUserTest() throws UserNotFoundException {
        var loadedUser = userService.findUserByNameAndRealm(TEST_USER_NAME, StoredUserDetails.DEFAULT_REALM);
        assertNotNull(loadedUser);
    }
    
    @Test
    @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
    public void changeRoleValueAndStoreTest() throws UserNotFoundException {
        var loadedUser = userService.findUserByNameAndRealm(TEST_USER_NAME, StoredUserDetails.DEFAULT_REALM);
        final String testRole = "abbbccc";
        loadedUser.setRole(testRole);
        userService.storeUser(loadedUser);
        loadedUser = userService.findUserByNameAndRealm(TEST_USER_NAME, StoredUserDetails.DEFAULT_REALM);
        assertEquals(testRole, loadedUser.getRole(), "The role was not changed inside the datbase.");
    }
    
    // check duplicate: DataIntegrityViolationException
    // check 
}
