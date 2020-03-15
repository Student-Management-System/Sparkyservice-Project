package net.ssehub.sparkyservice.api.integration.storeduser;

import static net.ssehub.sparkyservice.api.conf.ControllerPath.MANAGEMENT_ADD_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.ssehub.sparkyservice.api.storeduser.IStoredUserService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // clears database after each test
public class StoredUserControllerRestIT {

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
    @Test
    @WithMockUser(value = "user") //username="spring"
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
    @Test
    @WithMockUser(value = "user")
    public void addUserAdminSuccessTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        MvcResult result = this.mvc
            .perform(put(MANAGEMENT_ADD_USER)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(content)
            .accept(MediaType.TEXT_PLAIN))
            .andReturn();
        assumeTrue(result.getResponse().getStatus() != 403);
        
        assertAll(
                () -> assertEquals(200, result.getResponse().getStatus()),
                () -> assertNotNull(userService.findUserByid(1))
            );
    }

    @Test
    @WithMockUser(value = "nonAdminUser")
    public void securityAddUserNonAdminTest() throws Exception {
        String content  = Files.readString(Paths.get("src/test/resources/dtoJsonFiles/NewUserDto.json.txt"));
        this.mvc
            .perform(put(MANAGEMENT_ADD_USER)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(content)
            .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isForbidden());
    }

    @Test
    public void securityEditUserGuestTest() throws Exception {
        
    }
}
