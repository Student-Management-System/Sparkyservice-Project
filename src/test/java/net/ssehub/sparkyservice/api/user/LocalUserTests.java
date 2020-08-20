package net.ssehub.sparkyservice.api.user;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.creation.AbstractSparkyUserFactory;

/**
 * Test class for {@link SparkyUser#equals(Object)} and {@link SparkyUser#hashCode()}.
 * 
 * @author marcel
 */
//checkstyle: stop exception type check
public class LocalUserTests {

    /**
     * Test if two different user objects are equal when they have the same fields. (Test for custom equal method)
     * 
     * @param factoryClass - The current Factory Class to test with
     * @throws Exception 
     */
    @ParameterizedTest
    @ValueSource(classes = {LocalUserFactory.class, LdapUserFactory.class, MemoryUserFactory.class})
    public void equalityTest(Class<AbstractSparkyUserFactory<?>> factoryClass) throws Exception {
        AbstractSparkyUserFactory<?> factory = factoryClass.getDeclaredConstructor().newInstance();
        var pw1 = new Password("hallo", "plain");
        var pw2 = new Password(pw1);
        var user1 = factory.create("test", pw1, UserRole.ADMIN, false);
        var user2 = factory.create("test", pw2, UserRole.ADMIN, false);
        
        assertTrue(user1.equals(user2));
    }

    @ParameterizedTest
    @ValueSource(classes = {LocalUserFactory.class, LdapUserFactory.class, MemoryUserFactory.class})
    public void unEqualityTest(Class<AbstractSparkyUserFactory<?>> factoryClass) throws Exception {
        AbstractSparkyUserFactory<?> factory = factoryClass.getDeclaredConstructor().newInstance();
        var pw1 = new Password("hallo", "plain");
        var pw2 = new Password(pw1);
        var user1 = factory.create("test", pw1, UserRole.ADMIN, false);
        var user2 = factory.create("test", pw2, UserRole.ADMIN, false);
        user2.setFullname("other");
        
        assertFalse(user1.equals(user2));
    }

    /**
     * Tests if the hash code is the same when two different objects have the same fields.
     * 
     * @param factoryClass - The current Factory Class to test with
     * @throws Exception 
     */
    @ParameterizedTest
    @ValueSource(classes = {LocalUserFactory.class, LdapUserFactory.class, MemoryUserFactory.class})
    public void hashCodeTest(Class<AbstractSparkyUserFactory<?>> factoryClass) throws Exception {
        AbstractSparkyUserFactory<?> factory = factoryClass.getDeclaredConstructor().newInstance();
        var pw1 = new Password("hallo", "plain");
        var pw2 = new Password(pw1);
        var user1 = factory.create("test", pw1, UserRole.ADMIN, false);
        var user2 = factory.create("test", pw2, UserRole.ADMIN, false);
        
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
