//package net.ssehub.sparkyservice.db.user;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//import org.hibernate.Session;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import net.ssehub.sparkyservice.db.hibernate.SessionFactoryHelper;
//import net.ssehub.sparkyservice.db.user.StoredUser;
//
//public class StoredUserIT {
//    private Session singleSession;
//    private StoredUser testUser;
//
//    @BeforeEach
//    public void _setUpSession() {
//        singleSession = SessionFactoryHelper.SESSION_FACTORY.getCurrentSession();
//        singleSession.beginTransaction();
//        var newPassword = new Password("Hallo");
//        var newUser = new StoredUser("testuser", newPassword, "TEST", true, "DEFAULT");
//        singleSession.save(newUser);
//        singleSession.getTransaction().commit();
//        singleSession = SessionFactoryHelper.SESSION_FACTORY.getCurrentSession();
//    }
//
//    @AfterAll
//    public static void _tearDown() {
//        SessionFactoryHelper.shutdown();
//    }
//
//    @Test
//    @Disabled
//    public void isUserStoredTest() {
//        singleSession.beginTransaction();
//        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
//        singleSession.getTransaction().commit();
//        assertNotNull(loadedUser);
//    }
//
//    @Test
//    @Disabled
//    public void userNameStoredTest() {        
//        singleSession.beginTransaction();
//        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
//        singleSession.getTransaction().commit();
//        assertEquals("User was not correctly stored into database.", loadedUser.userName, testUser.userName);
//    }
//
//    @Test
//    @Disabled
//    public void isPasswordAssosiatedTest() {
//        singleSession.beginTransaction();
//        StoredUser loadedUser = singleSession.load(StoredUser.class, 1);
//        singleSession.getTransaction().commit();
//        assertNotNull(loadedUser.passwordEntity, "Password was not found in database but should be saved.");
//    }
//}
