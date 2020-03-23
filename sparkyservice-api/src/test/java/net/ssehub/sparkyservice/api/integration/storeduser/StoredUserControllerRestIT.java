package net.ssehub.sparkyservice.api.integration.storeduser;

import static net.ssehub.sparkyservice.api.conf.ControllerPath.MANAGEMENT_ADD_USER;
import static net.ssehub.sparkyservice.api.conf.ControllerPath.MANAGEMENT_EDIT_USER;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.ssehub.sparkyservice.api.storeduser.IStoredUserService;
import net.ssehub.sparkyservice.api.storeduser.StoredUserController;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto;
import net.ssehub.sparkyservice.api.testconf.AbstractContainerDatabaseTest;
import net.ssehub.sparkyservice.api.testconf.IntegrationTest;
import net.ssehub.sparkyservice.api.testconf.TestUserConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes= {TestUserConfiguration.class})
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // clears database
//@ContextConfiguration(classes= {UnitTestDataConfiguration.class, SecurityConfig.class})
public class StoredUserControllerRestIT extends AbstractContainerDatabaseTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired 
    private IStoredUserService userService; 

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
            .perform(put(MANAGEMENT_ADD_USER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk());
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
            .perform(put(MANAGEMENT_ADD_USER)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(content)
            .accept(MediaType.TEXT_PLAIN))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() != 403, "Admin is not authorized, can't add a new user");
        
        assertAll(
                () -> assertEquals(200, result.getResponse().getStatus(), "Wrong response status: Expected OK as "
                        + "response for adding a new user"),
                () -> assertNotNull(userService.findUserById(1), "Status was OK, but no user was saved to database")
            );
    }

    @IntegrationTest
    @WithMockUser(username = "nonAdminUser", roles = "DEFAULT")
    public void securityAddUserNonAdminTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        this.mvc
            .perform(put(MANAGEMENT_ADD_USER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden());
    }

    /**
     * A guest user (a user which isn't authenticated) should not access the editing controller.
     * 
     * @throws Exception
     */
    @IntegrationTest
    public void securityEditUserGuestTest() throws Exception {
        this.mvc
            .perform(
                put(MANAGEMENT_ADD_USER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("")
                    .accept(MediaType.TEXT_PLAIN))
             .andExpect(status().isForbidden());
    }

    /**
     * Test for {@link StoredUserController#editLocalUser(EditUserDto, UserDetails)}. <br>
     * Tests if a user can edit his own data. For mocking the user a specific test DetailsService is used:
     * {@link TestUserConfiguration#defaultDetailsService() which authenticates a user in the DEFAULT_REALM and with 
     * username "testuser". This configuration must match with the values of EditUserDto.json.txt
     * 
     * @throws Exception
     */
    @IntegrationTest 
    @WithUserDetails(value = "testuser", userDetailsServiceBeanName="defaultUserService")
    public void editUserSelfSuccessTest() throws Exception {
        /*
         * A user can only edit the same username + realm which match which the values of the 
         * authenticated user (as long he is not an admin). The username and realm must match!
         */
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/EditUserDto.json.txt"));
        this.mvc
            .perform(put(MANAGEMENT_EDIT_USER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk());
    }

    /**
     * Tests that a user can't edit other users value (when he does not have the role "Admin").
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithUserDetails(value = "testuser123", userDetailsServiceBeanName="defaultUserService") // must NOT match with values from EditUserDto.json.txt
    public void editOtherUsersNegativeTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/EditUserDto.json.txt"));
        this.mvc
            .perform(put(MANAGEMENT_EDIT_USER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content)
                    .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden());
    }

    /**
     * Tests if an admin can edit the data of other users.
     * 
     * @throws Exception
     */
    @IntegrationTest
    @WithMockUser(value = "user")
    @Disabled
    public void editUserAdminSuccessTest() throws Exception {
        fail("Not implemented");
    }
}
