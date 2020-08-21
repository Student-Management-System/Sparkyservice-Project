package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Provides methods for user data modification with default (=user) permissions. 
 * 
 * @author marcel
 */
@Service
class DefaultUserModificationImpl implements UserModificationService {

    /**
     * {@inheritDoc}.
     */
    @Override
    public void update(SparkyUser databaseUser, UserDto userDto) {
        if (databaseUser != null && userDto != null) {
            applyPersonalSettingsDto(databaseUser, notNull(userDto.settings));
            final var pwDto = userDto.passwordDto; 
            if (pwDto != null) {
                databaseUser.updatePassword(pwDto, UserRole.DEFAULT);
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public UserDto asDto(SparkyUser user) {
        var dto = new UserDto();
        dto.realm = user.getRealm();
        dto.role = user.getRole();
        dto.settings = settingsAsDto(user.getSettings());
        dto.username = user.getUsername();
        dto.fullName = user.getFullname();
        return dto;
    }

    /**
     * Changes the values of a users settings. Payload is not changed since users do not have permissions to do this.
     * 
     * @param user
     * @param settings
     */
    protected static void applyPersonalSettingsDto(@Nonnull SparkyUser user, @Nonnull SettingsDto settings) {
        PersonalSettings dbSettings = user.getSettings();
        dbSettings.setEmail_address(settings.emailAddress);
        dbSettings.setWantsAi(settings.wantsAi);
        dbSettings.setEmail_receive(settings.emailReceive);
    }

    /**
     * Creates a dto based on the values of settings without {@link PersonalSettings#getPayload()}.
     * 
     * @param settings
     * @return May incomplete DTO of settings
     */
    protected static SettingsDto settingsAsDto(PersonalSettings settings) {
        var dto = new SettingsDto();
        dto.emailAddress = settings.getEmail_address();
        dto.emailReceive = settings.isEmail_receive();
        dto.wantsAi = settings.isWantsAi();
        return dto;
    }

}
