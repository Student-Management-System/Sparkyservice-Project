package net.ssehub.sparkyservice.api.integration.user;

import static net.ssehub.sparkyservice.api.testconf.SparkyAssertions.assertDtoEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.AbstractContainerTestDatabase;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.testconf.TestUserConfiguration;
import net.ssehub.sparkyservice.api.user.IUserService;
import net.ssehub.sparkyservice.api.user.UserController;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes= {TestUserConfiguration.class})
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // clears database
//@ContextConfiguration(classes= {UnitTestDataConfiguration.class, SecurityConfig.class})
public class UserControllerRestIT extends AbstractContainerTestDatabase {

    @Autowired
    private WebApplicationContext context;

    @Autowired 
    private IUserService userService; 

    private MockMvc mvc;

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
    @WithMockUser(username="admin", roles = "ADMIN")
    public void securityAddUserAdminAccessTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        this.mvc
            .perform(put(ControllerPath.USERS_PUT)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
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
    @WithMockUser(username="admin", roles = "ADMIN")
    public void addUserAdminSuccessTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        MvcResult result = this.mvc
            .perform(put(ControllerPath.USERS_PUT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(content)
            .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() != 403, "Admin is not authorized, can't add a new user");
        
        assertAll(
                () -> assertEquals(201, result.getResponse().getStatus(), "Wrong response status: "
                        + "Expected CREATED as response for adding a new user"),
                () -> assertNotNull(userService.findUserById(1), "Status was OK, but no user was saved to database")
            );
    }

    @IntegrationTest
    @WithMockUser(username = "nonAdminUser", roles = "DEFAULT")
    public void securityAddUserNonAdminTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        this.mvc
            .perform(put(ControllerPath.USERS_PUT)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden());
    }

    /**
     * Tests if non-admin user is forbidden to delete other users.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(username="admin", roles = "DEFAULT")
    public void securityNonAdminDeleteTest() throws Exception {
        var user = new User("testuser", null, UserRealm.LDAP, true, UserRole.DEFAULT);
        userService.storeUser(user);
        assumeTrue(userService.isUserInDatabase(user));
        
        this.mvc
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.LDAP, "testuser")
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
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.LDAP, "testuser")
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
    @WithMockUser(username="admin", roles = "ADMIN")
    public void functionAdminDeleteTest() throws Exception {
        var user = new User("testuser", null, UserRealm.LDAP, true, UserRole.DEFAULT);
        userService.storeUser(user);
        assumeTrue(userService.isUserInDatabase(user));
        
        this.mvc
        .perform(delete(ControllerPath.USERS_DELETE, UserRealm.LDAP, "testuser")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
        assertFalse(userService.isUserInDatabase(user));
    }

    @IntegrationTest
    @WithMockUser(username="admin", roles = "ADMIN")
    public void functionGetSingleUserTest() throws Exception {
        var user = new User("testuser", null, UserRealm.LDAP, true, UserRole.DEFAULT);
        userService.storeUser(user);
        assumeTrue(userService.isUserInDatabase(user));
        
        MvcResult result = this.mvc
                .perform(get(ControllerPath.USERS_GET_SINGLE, UserRealm.LDAP, "testuser")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        
        assertEquals(200, result.getResponse().getStatus());
        String dtoString = result.getResponse().getContentAsString();
        assertDoesNotThrow(() -> new ObjectMapper().readValue(dtoString, UserDto.class), 
                "Some wrong values was returned from the controller. The content is not a valid json dto"); 
        var returnedUserDto =  new ObjectMapper().readValue(dtoString, UserDto.class);
        assertDtoEquals(user.asDto(), returnedUserDto);
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
    @WithMockUser(username="admin", roles = "ADMIN")
    public void functionGetAllTest() throws Exception {
        var user = new User("testuser", null, UserRealm.LDAP, true, UserRole.DEFAULT);
        userService.storeUser(user);
        assumeTrue(userService.isUserInDatabase(user));
        var user2 = new User("testuser2", null, UserRealm.LDAP, true, UserRole.DEFAULT);
        userService.storeUser(user2);
        assumeTrue(userService.isUserInDatabase(user2));
        
        MvcResult result = this.mvc
                .perform(get(ControllerPath.USERS_GET_ALL)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();
        assumeTrue(result.getResponse().getStatus() == 200);
        
        String dtoArrayString = result.getResponse().getContentAsString();
        var dtoArray = new ObjectMapper().readValue(dtoArrayString, UserDto[].class);
        assertEquals(2, dtoArray.length);
    }
}
