package net.ssehub.sparkyservice.api.integration.user;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.testconf.TestUserConfiguration;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserController;
import net.ssehub.sparkyservice.api.user.UserController.UsernameDto;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Test for {@link UserController} - permissions and add functions.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {TestUserConfiguration.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace=Replace.AUTO_CONFIGURED)
//checkstyle: stop exception type check
public class UserControllerRestIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired 
    private UserStorageService userService; 

    private MockMvc mvc;

    /**
     * Creates a testuser in the storage for testing purposes with username <code>testuser</code>.
     * 
     * @param service
     * @return a Testuser which is in storage
     */
    private static SparkyUser createTestUserInStorage(UserStorageService service) {
        var user = UserRealm.UNIHI.getUserFactory().create("testuser", null, UserRole.DEFAULT, true);
        service.commit(user);
        assumeTrue(service.isUserInStorage(user));
        return user;
    }

    /**
     * Setup is run before each tests and initialize the web context for mocking.
     */
    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
          .webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }

    /**
     * Test for {@link StoredUserController#addLocalUser(net.ssehub.sparkyservice.api.storeduser.dto.NewUserDto).
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void securityAddUserAdminAccessTest() throws Exception {
        UsernameDto username = new UsernameDto();
        username.username = "testuser";
        this.mvc
            .perform(put(ControllerPath.USERS_PUT)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(new ObjectMapper().writeValueAsString(username))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());
    }

    /**
     * Test for {@link StoredUserController#addLocalUser(net.ssehub.sparkyservice.api.storeduser.dto.NewUserDto).
     * Tests if the controller stores something in the database. This test assumes that the permission 
     * to reach the controller are given (there is a dedicated test for it).
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void addUserAdminSuccessTest() throws Exception {
        UsernameDto username = new UsernameDto();
        username.username = "testuser";
        MvcResult result = this.mvc
            .perform(put(ControllerPath.USERS_PUT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(new ObjectMapper().writeValueAsString(username))
            .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() != 403, "Admin is not authorized, can't add a new user");
        
        assertAll(
            () -> assertEquals(201, result.getResponse().getStatus(), "Wrong response status: "
                    + "Expected CREATED as response for adding a new user"),
            () -> assertNotNull(userService.findUser(1), "Status was OK, but no user was saved to database")
        );
    }

    /**
     * Security test for {@link UserController#createLocalUser(net.ssehub.sparkyservice.api.user.dto.NewUserDto)}. 
     * Tests if the access is denied for non-admin users which tries to add a new local user. 
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "nonAdminUser", roles = "DEFAULT")
    public void securityAddUserNonAdminTest() throws Exception {
        UsernameDto username = new UsernameDto();
        username.username = "testuser";
        this.mvc
            .perform(put(ControllerPath.USERS_PUT)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(new ObjectMapper().writeValueAsString(username))
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden());
    }

    /**
     * Tests if non-admin user is forbidden to delete other users.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "DEFAULT")
    public void securityNonAdminDeleteTest() throws Exception {
        var user = UserRealm.UNIHI.getUserFactory().create("testuser", null, UserRole.DEFAULT, true);
        userService.commit(user);
        assumeTrue(userService.isUserInStorage(user));
        
        this.mvc
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.UNIHI, "testuser")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden()); 
    }

    /**
     * Test for {@link UserController#deleteUser(UserRealm, String)}.
     * <br>
     * A non authenticated user (guests) should not be able to reach the controller (return status 403 - forbidden).
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void securityGuestDeleteTest() throws Exception {
        this.mvc
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.UNIHI, "testuser")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden()); 
    }

    /**
     * Tests if an administrator can delete other users via {@link UserController#deleteUser(UserRealm, String)}.
     * <br>
     * <br> For this test a user must be successful written to the database. It will skips if this is not working.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void functionAdminDeleteTest() throws Exception {
        var user = createTestUserInStorage(userService);
        
        this.mvc
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.UNIHI, "testuser")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
        assertFalse(userService.isUserInStorage(user));
    }

    /**
     * Function test for 
     * {@link UserController#getSingleUser(UserRealm, String, org.springframework.security.core.Authentication)}.
     * Tries to get different single user via User controller as authenticated admin 
     * (this tests the authorization feature too).
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void functionGetSingleUserTest() throws Exception {
        var user = createTestUserInStorage(userService);
        
        MvcResult result = this.mvc
                .perform(get(ControllerPath.USERS_GET_SINGLE, "unused", user.getIdentity().asUsername()) //TODO remove second path variable
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
        
        assertEquals(200, result.getResponse().getStatus(), "A single user couldn't loaded via controller - permission "
                + "problems ? ");
        String dtoString = result.getResponse().getContentAsString();
        assertDoesNotThrow(() -> new ObjectMapper().readValue(dtoString, UserDto.class), 
                "Some wrong values was returned from the controller. The content is not a valid json dto"); 
        var returnedUserDto =  new ObjectMapper().readValue(dtoString, UserDto.class);
        UserDto userDto = UserRole.ADMIN.getPermissionTool().asDto(user);
        assertEquals(userDto, returnedUserDto);
    }

    /**
     * Tests if the getAll controller functions returns the correct amount of users in the database. 
     * (Does not check the content of the list).
     * <br><br>
     * To avoid an additional user object in the database, we don't mock the user with user 
     * {@link TestUserConfiguration#adminDetailsService()}.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void functionGetAllTest() throws Exception {
        var factory = UserRealm.UNIHI.getUserFactory();
        var user1 = factory.create("testuser", null, UserRole.DEFAULT, true);
        var user2 = factory.create("testuser2", null, UserRole.DEFAULT, true);
        userService.commit(user1);
        assumeTrue(userService.isUserInStorage(user1));
        userService.commit(user2);
        assumeTrue(userService.isUserInStorage(user2));
        
        MvcResult result = this.mvc
                .perform(get(ControllerPath.USERS_GET_ALL)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200);
        
        String dtoArrayString = result.getResponse().getContentAsString();
        var dtoArray = new ObjectMapper().readValue(dtoArrayString, UserDto[].class);
        assertEquals(2, dtoArray.length);
    }
    
    @IntegrationTest
    public void heartbeatTest() throws Exception {
        this.mvc.perform(get(ControllerPath.HEARTBEAT)).andExpect(status().isNoContent());
    }
}
