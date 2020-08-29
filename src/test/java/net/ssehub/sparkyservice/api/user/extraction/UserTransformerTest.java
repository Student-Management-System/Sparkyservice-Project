package net.ssehub.sparkyservice.api.user.extraction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.LocalUserFactory;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.TestingUserRepository;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.NullHelpers;

@ExtendWith(SpringExtension.class) 
@ContextConfiguration(classes = {UnitTestDataConfiguration.class})
public class UserTransformerTest {

    static class TestPrincipal implements SparkysAuthPrincipal {
        @Override
        public @Nonnull String getName() {
            return "testUser";
        }

        @Override
        public @Nonnull UserRealm getRealm() {
            return UserRealm.LOCAL;
        }

        @Override
        @Nonnull
        public String asString() {
            return "";
        }
    }

    @Autowired
    private UserExtractionService transformer;

    /**
     * Maybe the used transformer implementation have to do database operations. For that case, each 
     * test case should provided a mocked repository function.
     */
    @MockBean
    private TestingUserRepository mockedRepository;

    @Test
    public void extendFromSpringUserDetails() throws UserNotFoundException, MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var userDetails = new org.springframework.security.core.userdetails.User("testuser", "testpass", Arrays.asList(authority));
        SparkyUser extendedUser = transformer.extractAndRefresh(userDetails); // in reality this is maybe null!
        assertAll(
            () -> assertEquals("testuser", extendedUser.getUsername()),
            () -> assertEquals(UserRealm.MEMORY, extendedUser.getRealm())
        );
    }

    @Test
    public void extendFromSparkyPrincipalTest() {
        var user = (SparkyUser) LocalUserDetails.newLocalUser("testUser", "test", UserRole.DEFAULT);
        var optUser = Optional.ofNullable(user.getJpa());
        when(mockedRepository.findByuserNameAndRealm("testUser", UserRealm.LOCAL)).thenReturn(optUser);
        assertDoesNotThrow(() -> transformer.extendAndRefresh(new TestPrincipal()));
    }

    @Test
    public void extendFromUserDto() throws MissingDataException {
        var dto = new UserDto();
        dto.realm = UserRealm.LOCAL;
        dto.username = "testUser";
        dto.role = UserRole.DEFAULT;
        var user = (SparkyUser) LocalUserDetails.newLocalUser("testUser", "test", UserRole.DEFAULT);
        var optUser = Optional.ofNullable(user.getJpa());
        when(mockedRepository.findByuserNameAndRealm("testUser", UserRealm.LOCAL)).thenReturn(optUser);
        
        var extendedUser = transformer.extractAndRefresh(dto);
        assertAll(
            () -> assertEquals(dto.role, extendedUser.getRole()),
            () -> assertEquals(dto.username, extendedUser.getUsername()),
            () -> assertEquals(dto.realm, extendedUser.getRealm())
        );
    }

    @Test
    public void userNameTokenRealmTest() throws MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(new TestPrincipal(), "test", Arrays.asList(authority));
        var extractedUser = NullHelpers.notNull(transformer.extract(token));
        assertAll(
            () -> assertEquals(authority.toString(), extractedUser.getRole().getAuthority()),
            () -> assertEquals(UserRealm.LOCAL, extractedUser.getRealm()),
            () -> assertEquals("test", extractedUser.getPassword())
        );
    }

    /**
     * Tests if SparkyUser is extracted when it's present as principal.
     */
    @Test
    public void extractSparkyUserPrincipalTest() {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var user = new LocalUserFactory().create("test", new Password("hallo", "plain"), UserRole.ADMIN, true);
        var token = new UsernamePasswordAuthenticationToken(user, "test", Arrays.asList(authority));
        var extractedUser = NullHelpers.notNull(transformer.extract(token));
        assertTrue(user.equals(extractedUser));
    }

    @Test
    public void userNameTokenTest() throws MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var token = new UsernamePasswordAuthenticationToken("user", "test", Arrays.asList(authority));
        assertThrows(MissingDataException.class, () -> transformer.extractAndRefresh(token));
    }
}
