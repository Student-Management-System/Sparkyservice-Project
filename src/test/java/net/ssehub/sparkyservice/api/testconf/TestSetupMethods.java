package net.ssehub.sparkyservice.api.testconf;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.LdapRealm;
import net.ssehub.sparkyservice.api.user.LocalRealm;
import net.ssehub.sparkyservice.api.user.MemoryRealm;
import net.ssehub.sparkyservice.api.user.RealmRegistry;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * Spring configuration class which provides a set of beans which should be used during (unit) testing. In order to use
 * it, use the following annotation: <br>
 * <code> @ContextConfiguration(classes= {UnitTestDataConfiguration.class}) </code>
 *
 * @author Marcel
 */
public class TestSetupMethods {

    public static final String NEW_PASSWORD = "testPassword";
    public static final String OLD_PASSWORD = "oldPw123";
    public static final String USER_EMAIL = "info@test";
    public static final String PAYLOAD = "testPayload";
    public static final String NICK_NAME = "user";
    public static final Identity IDENT = new Identity(NICK_NAME, new DummyRealm("Dummy"));
    public static final String USER_NAME = IDENT.asUsername();
    public static final LocalDate EXP_DATE = LocalDate.now().plusDays(2);

    /**
     * Creates a simple and complete {@link UserDto} object for testing purposes.
     * 
     * @return complete testing dto
     */
    public static @Nonnull UserDto createExampleDto() {
        testRealmSetup(IDENT.realm());
        var editUserDto = new UserDto();
        editUserDto.username = USER_NAME;
        editUserDto.passwordDto = new ChangePasswordDto();
        editUserDto.passwordDto.newPassword = NEW_PASSWORD;
        editUserDto.passwordDto.oldPassword = OLD_PASSWORD;
        editUserDto.settings = new SettingsDto();
        editUserDto.settings.payload = PAYLOAD;
        editUserDto.settings.emailAddress = USER_EMAIL;
        editUserDto.settings.emailReceive = true;
        editUserDto.settings.wantsAi = true;
        editUserDto.expirationDate = EXP_DATE;
        return editUserDto;
    }

    @Nonnull
    public static List<UserRealm> testRealmSetup(UserRealm... realms) {
        var list = NullHelpers.notNull(List.of(realms));
        new RealmRegistry(list);
        return list;
    }
    
    public static List<UserRealm> allTestRealmSetup() {
        var ldapRealm = new LdapRealm();
        var localRealm = new LocalRealm();
        var memoryRealm = new MemoryRealm();
        return testRealmSetup(ldapRealm, localRealm, memoryRealm);
    }
}
