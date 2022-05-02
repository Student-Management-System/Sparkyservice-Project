package net.ssehub.sparkyservice.api.routing;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.ssehub.sparkyservice.api.config.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.testconf.TestSetupMethods;

/**
 * Test class for {@link AccessControlListInterpreter}.
 *
 * @author marcel
 */
public class AccessControlListInterpreterTests {

    private static final String CONFIGURED_PATH = "testpath";
    private ZuulRoutes zuulRoutes;

    /**
     * Setup method runs before each test. Creates an example {@link ZuulRoutes} accessable by {@link #zuulRoutes}.
     */
    @BeforeEach
    public void _zuulSetup() {
        var routes = new TreeMap<String, String>();
        routes.put(CONFIGURED_PATH + ".acl", "test@LOCAL");
        routes.put(CONFIGURED_PATH + ".url", "https://google.com");
        routes.put("other" + ".acl", "test2@LOCAL"); // just for list emulation
        routes.put("other" + ".url", "https://google.com");
        zuulRoutes = new ZuulRoutes();
        zuulRoutes.setRoutes(routes);
        TestSetupMethods.allTestRealmSetup();
    }

    /**
     * Test if the {@link AccessControlListInterpreter#isAclEnabled()} is true when a correct acl configuration 
     * is available for a given ressource.
     */
    @Test
    @DisplayName("Test if ACL is enabled when it's configured")
    public void isAclEnabledTest() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH);
        assertTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");
    }

    @ParameterizedTest
    @ValueSource(strings = {CONFIGURED_PATH, ""})
    @DisplayName("Test if ACL is NOT enabled when it's NOT configured and all users allowed")
    public void isAclNotEnabledTest(String currentPath) {
        var interpreter = new AccessControlListInterpreter(null, currentPath);
        assertAll(
            () -> assertFalse(interpreter.isAclEnabled(), "ACL is not configured but interpreter says it is"),
            () -> assertTrue(interpreter.isAllowed("anything@LDAP"), "Everybody should be allowed")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {CONFIGURED_PATH, ""})
    @DisplayName("Configured path with none as ACL expteded ACL is disabled and all users allowed")
    public void isAclNotEnabledWhenNoneTest(String currentPath) {
        var routes = new TreeMap<String, String>();
        routes.put(CONFIGURED_PATH + ".url", "https://google.com");
        routes.put(CONFIGURED_PATH + ".acl", "none");
        var zuulRoutes = new ZuulRoutes();
        zuulRoutes.setRoutes(routes);
        var interpreter = new AccessControlListInterpreter(zuulRoutes, currentPath);
        assertAll(
            () -> assertFalse(interpreter.isAclEnabled(), "ACL configured with none but interpreter says it's is"),
            () -> assertTrue(interpreter.isAllowed("anything@LDAP"), "Everybody should be allowed to "
                    + "access")
        );
    }

    /**
     * Tests if a user is allowed to pass / Test if the acl is disabled when:<br>
     * the acl configuration is missing but the path is configured. 
     * The default setting should be, allow all connections and all users.
     * 
     * @param currentPath
     */
    @ParameterizedTest
    @ValueSource(strings = {CONFIGURED_PATH, ""})
    @DisplayName("Test user is allowed when path configured but acl-conf missing")
    public void aclFailesWhenMissingConfigurationEntry(String currentPath) {
        var routes = new TreeMap<String, String>();
        routes.put(CONFIGURED_PATH + ".url", "https://google.com");
        var zuulRoutes = new ZuulRoutes();
        zuulRoutes.setRoutes(routes);
        var interpreter = new AccessControlListInterpreter(zuulRoutes, currentPath);
        assertAll(
            () -> assertFalse(interpreter.isAclEnabled()),
            () -> assertTrue(interpreter.isAllowed("all@LDAP"))
        );
    }

    /**
     * Test that ACL state is not enabled when ZuulRoutes provided but no concrete routes. 
     * (As long as an auto configuration mapper is used, this should never happen in reality). 
     */
    @ParameterizedTest
    @ValueSource(strings = {CONFIGURED_PATH, ""})
    @DisplayName("Test with existing Zuul Routes but with concrete null routes")
    public void existingZuulRoutesWithoutConcreteRoutes(String currentPath) {
        var zuulRoutes = new ZuulRoutes();
        zuulRoutes.setRoutes(null);
        var interpreter = new AccessControlListInterpreter(zuulRoutes, currentPath);
        assertAll(
            () -> assertFalse(interpreter.isAclEnabled()),
            () -> assertTrue(interpreter.isAllowed("any@LDAP"))
        );
    }

    /**
     * When the ACL is configured with usernames, a use which is on it should be allowed.
     */
    @Test
    @DisplayName("Test is allowed when it's configured")
    public void isUserNameAllowedPositivTest() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH);
        assumeTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");

        assertTrue(interpreter.isAllowed("test@LOCAL"));
    }

    /**
     * When the ACL is configured with usernames, a user which is not on it, shouldn't be allowed.
     */
    @Test
    @DisplayName("Test user is not allowed to access")
    public void isUserNameAllowedNegativeTest() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH);
        assumeTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");

        assertFalse(interpreter.isAllowed("something@LDAP"), 
                "User is allowed to access even he's not on the allowed list");
    }

    /**
     * Tests if the correct configured path is returned.
     */
    @Test
    public void configuredPathTest() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH);
        assertEquals(CONFIGURED_PATH, interpreter.getConfiguredPath());
    }

    /**
     * Tests if the correct configured path is returned.
     */
    @Test
    @DisplayName("Path with trailing slash test")
    public void pathTestWithSlash() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, "/test/");
        assertEquals("test", interpreter.getConfiguredPath());
    }

    /**
     * When multiple paths are configured, the ACL interpreter should only read the configuration for one 
     * path at a time and only those users should be allowed to access them. 
     */
    @Test
    @DisplayName("User allowed for multiple configured paths test")
    public void userNameAllowedForDifferentPathTest() {
        zuulRoutes.getRoutes().put("otherpath.url", "https://google.com");
        zuulRoutes.getRoutes().put("otherpath.acl", "user@MEMORY");
        
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH);
        var interpreterOtherPath = new AccessControlListInterpreter(zuulRoutes, "otherpath");
        assumeTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");
        assumeTrue(interpreterOtherPath.isAclEnabled(), "ACL is enabled but interpreter says it's not");
        
        assertAll(
            () -> assertFalse(interpreterOtherPath.isAllowed("test@LOCAL")),
            () -> assertTrue(interpreter.isAllowed("test@LOCAL")),
            () -> assertTrue(interpreterOtherPath.isAllowed("user@MEMORY")),
            () -> assertFalse(interpreter.isAllowed("user@MEMORY"))
        );
    }

    @Test
    @DisplayName("A path with an absolute path and not just the configured route test")
    public void aclEnabledForLongPath() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH + "/other/test/path/");
        assumeTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");
    }

    @Test
    @DisplayName("A path with an absolute path and not just the configured route test")
    public void aclEnablesdForLongPath() {
        var interpreter = new AccessControlListInterpreter(zuulRoutes, CONFIGURED_PATH + "/test");
        assumeTrue(interpreter.isAclEnabled(), "ACL is enabled but interpreter says it's not");
    }
}
