package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;

@Service
class DefaultUserModificationImpl implements UserModifcationService {

    @Override
    public void changePasswordFromDto(@Nullable User databaseUser, @Nullable ChangePasswordDto passwordDto) {
        if (databaseUser != null && passwordDto != null && passwordDto.oldPassword != null) {
            final String localNewPassword = passwordDto.newPassword;
            if (localNewPassword != null) {
                Optional.of(databaseUser)
                .filter(LocalUserDetails.class::isInstance)
                .map(LocalUserDetails.class::cast)
                .filter(u -> u.getEncoder().matches(passwordDto.oldPassword, u.getPassword()))
                .ifPresent(u -> UserModifcationService.applyPassword(notNull(u), localNewPassword));
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void changeUserValuesFromDto(User databaseUser, UserDto userDto) {
        if (databaseUser != null && userDto != null) {
            applyPersonalSettingsDto(databaseUser, notNull(userDto.settings));
            changePasswordFromDto(databaseUser, userDto.passwordDto);
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public UserDto userAsDto(User user) {
        var dto = new UserDto();
        dto.realm = user.getRealm();
        dto.role = user.getRole();
        dto.settings = settingsAsDto(user.getProfileConfiguration());
        dto.username = user.getUserName();
        dto.fullName = user.getFullName();
        return dto;
    }

    /**
     * Changes the values of a users settings. Payload is not changed since users do not have permissions to do this.
     * 
     * @param user
     * @param settings
     */
    protected static void applyPersonalSettingsDto(@Nonnull User user, @Nonnull SettingsDto settings) {
        PersonalSettings dbSettings = user.getProfileConfiguration();
        dbSettings.setEmail_address(settings.email_address);
        dbSettings.setWantsAi(settings.wantsAi);
        dbSettings.setEmail_receive(settings.email_receive);
    }

    /**
     * Creates a dto based on the values of settings without {@link PersonalSettings#getPayload()}.
     * 
     * @param settings
     * @return May incomplete DTO of settings
     */
    protected static SettingsDto settingsAsDto(PersonalSettings settings) {
        var dto = new SettingsDto();
        dto.email_address = settings.getEmail_address();
        dto.email_receive = settings.isEmail_receive();
        dto.wantsAi = settings.isWantsAi();
        return dto;
    }

    @Override
    public <T extends User> void setPermissionProvider(User user) {
    }
}
