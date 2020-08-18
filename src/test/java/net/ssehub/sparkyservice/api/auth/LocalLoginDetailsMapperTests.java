package net.ssehub.sparkyservice.api.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Test class for {@link LocalLoginDetailsMapper}. 
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {UnitTestDataConfiguration.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class LocalLoginDetailsMapperTests {

    @Autowired
    private LocalLoginDetailsMapper mapper;

    @Autowired
    private UserStorageService storageService;

    /**
     * Tests if a username can be found and has the right type.
     */
    @Test
    public void loadByUserNameInstanceTest() {
        storageService.addUser("TESTNAME");
        assertDoesNotThrow(() -> mapper.loadUserByUsername("TESTNAME"));
    }

    /**
     * Tests with empty names.
     */
    @Test
    public void loadMissingNameTest() {
        assertThrows(UsernameNotFoundException.class, () -> mapper.loadUserByUsername(StringUtils.EMPTY));
    }

    
    /**
     * Negative tests. The correct exception should be thrown when the user does not exist in the database. 
     */
    @Test
    public void loadUserByNameNegativeTest() {
        assertThrows(UsernameNotFoundException.class, () -> mapper.loadUserByUsername("djshfdjkhs"));
    }

    /**
     * Tests if the method can handle null parameter.
     */
    @Test
    public void loadUserByNameNullTest() {
        assertThrows(UsernameNotFoundException.class, () -> mapper.loadUserByUsername(null));
    }

}
