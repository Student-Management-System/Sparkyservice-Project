package net.ssehub.sparkyservice.api.user.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;

/**
 * Test class for {@link ServiceAccStorageService}.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
public class ServiceAccStorageTest {

    @Autowired
    private ServiceAccStorageService serviceStorage;

    @Test
    public void getAllServiceAccountsTest() {
        assertTrue(serviceStorage.findAllServiceAccounts().isEmpty(), "When no service accounts available, list "
                + "should be empty.");
    }

//    @Test
//    public void getAllServiceAccountsTest() {
//        assertTrue(serviceStorage.findAllServiceAccounts().isEmpty(), "When no service accounts available, list "
//                + "should be empty.");
//    }
}
