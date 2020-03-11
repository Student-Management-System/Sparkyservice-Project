package net.ssehub.sparkyservice.db.user;
import static org.junit.Assert.*;

import org.hibernate.Session;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import net.ssehub.sparkyservice.db.hibernate.SessionFactoryHelper;
import net.ssehub.sparkyservice.db.user.StoredUser;

public class StoredUserIntegrationTests {
    private Session singleSession;
    private StoredUser testUser;

    @Before
    public void _setUpSession() {
        singleSession = SessionFactoryHelper.SESSION_FACTORY.getCurrentSession();
        singleSession.beginTransaction();
        var newPassword = new Password("Hallo");
        var newUser = new StoredUser("testuser", newPassword, "TEST", true, "DEFAULT");
        singleSession.save(newUser);
        singleSession.getTransaction().commit();
        singleSession = SessionFactoryHelper.SESSION_FACTORY.getCurrentSession();
    }

    @AfterClass
    public static void _tearDown() {
        SessionFactoryHelper.shutdown();
    }

    @Test
    @Ignore
    public void isUserStoredTest() {
        singleSession.beginTransaction();
        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
        singleSession.getTransaction().commit();
        assertNotNull(loadedUser);
    }

    @Test
    @Ignore
    public void userNameStoredTest() {        
        singleSession.beginTransaction();
        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
        singleSession.getTransaction().commit();
        assertEquals("User was not correctly stored into database.",  loadedUser.userName, testUser.userName);
    }

    @Test
    @Ignore
    public void isPasswordAssosiatedTest() {
        singleSession.beginTransaction();
        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
        singleSession.getTransaction().commit();
        assertNotNull("Password was not found in database but should be saved.", loadedUser.passwordEntity);
    }
}
