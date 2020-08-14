package net.ssehub.sparkyservice.api.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserRepository;
import net.ssehub.sparkyservice.api.user.transformation.MissingDataException;
import net.ssehub.sparkyservice.api.user.transformation.UserTransformerService;
import net.ssehub.sparkyservice.api.util.NullHelpers;

@ExtendWith(SpringExtension.class) 
@ContextConfiguration(classes= {UnitTestDataConfiguration.class})
public class UserTransformerTest {
    class TestPrincipal implements SparkysAuthPrincipal {
        @Override
        public @Nonnull String getName() {
            return "testUser";
        }

        @Override
        public @Nonnull UserRealm getRealm() {
            return UserRealm.LOCAL;
        }
    }

    @Autowired
    private UserTransformerService transformer;

    /**
     * Maybe the used transformer implementation have to do database operations. For that case, each 
     * test case should provided a mocked repository function.
     */
    @MockBean
    private UserRepository mockedRepository;

    @Test
    public void extendFromUserDetails() throws UserNotFoundException, MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var userDetails = new org.springframework.security.core.userdetails.User("testuser", "testpass", Arrays.asList(authority));
        User extendedUser = NullHelpers.notNull(transformer.extendFromAnyPrincipal(userDetails)); // in reality this is maybe null!
        assertAll(
                () -> assertTrue(extendedUser != null),
                () -> assertEquals("testuser", extendedUser.getUserName()),
                () -> assertEquals(UserRealm.MEMORY, extendedUser.getRealm())
            );
    }

    @Test
    public void extendFromSparkyPrincipalTest() {
        var user = (User) LocalUserDetails.newLocalUser("testUser", "test", UserRole.DEFAULT);
        var optUser = Optional.ofNullable(user);
        when(mockedRepository.findByuserNameAndRealm("testUser", UserRealm.LOCAL)).thenReturn(optUser);
        assertDoesNotThrow(() -> transformer.extendFromSparkyPrincipal(new TestPrincipal()));
    }

    @Test
    public void extendFromUserDto() throws MissingDataException {
        var dto = new UserDto();
        dto.realm = UserRealm.LOCAL;
        dto.username = "testUser";
        dto.role = UserRole.DEFAULT;
        var user = (User) LocalUserDetails.newLocalUser("testUser", "test", UserRole.DEFAULT);
        var optUser = Optional.ofNullable(user);
        when(mockedRepository.findByuserNameAndRealm("testUser", UserRealm.LOCAL)).thenReturn(optUser);
        
        var extendedUser = transformer.extendFromUserDto(dto);
        assertAll(
                () -> assertEquals(dto.role, extendedUser.getRole()),
                () -> assertEquals(dto.username, extendedUser.getUserName()),
                () -> assertEquals(dto.realm, extendedUser.getRealm())
            );
    }

    @Test
    public void userNameTokenRealmTest() throws MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(new TestPrincipal(), "test", Arrays.asList(authority));
        var extendedUser = NullHelpers.notNull(transformer.extendFromAuthentication(token));
        assertAll(
                () -> assertNotNull(extendedUser),
                () -> assertEquals(UserRealm.LOCAL, extendedUser.getRealm())
            );
    }

    @Test
    public void userNameTokenTest() throws MissingDataException {
        var authority = new SimpleGrantedAuthority(UserRole.FullName.ADMIN);
        var token = new UsernamePasswordAuthenticationToken("user", "test", Arrays.asList(authority));
        var extendedUser = NullHelpers.notNull(transformer.extendFromAuthentication(token));
        assertAll(
                () -> assertNotNull(extendedUser),
                () -> assertEquals(UserRealm.MEMORY, extendedUser.getRealm())
            );
    }
}
