package net.ssehub.sparkyservice.api.user.modification;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Utility class for modifying users with admin permissions.
 * 
 * @author marcel
 */
@Service
class AdminUserModificationImpl implements UserModifcationService {

    @Autowired
    private final UserModifcationService lowerPermService;

    /**
     * An admin modification service. Set a lower permissions service where this implementation can inherit permissions
     * from.
     * 
     * @param lowerPermService
     */
    @Autowired
    public AdminUserModificationImpl(UserModifcationService lowerPermService) {
        this.lowerPermService = lowerPermService;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void changeUserValuesFromDto(@Nullable User databaseUser, @Nullable UserDto userDto) {
        if (databaseUser != null && userDto != null) {
            final SettingsDto localDtoSettings = userDto.settings;
            final String localUserName = userDto.username;
            if (localDtoSettings != null && localUserName != null) {
                databaseUser.setUserName(localUserName);
                changePasswordFromDto(databaseUser, userDto.passwordDto);
                databaseUser.setExpirationDate(userDto.expirationDate);
                databaseUser.setFullName(userDto.fullName);
                databaseUser.setRole(userDto.role);
                DefaultUserModificationImpl.applyPersonalSettingsDto(databaseUser, localDtoSettings);
                databaseUser.getProfileConfiguration().setPayload(userDto.settings.payload);
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void changePasswordFromDto(@Nullable User databaseUser, @Nullable UserDto.ChangePasswordDto passwordDto) {
        if (databaseUser != null && passwordDto != null) {
            final String localNewPassword = passwordDto.newPassword;
            if (localNewPassword != null) {
                UserModifcationService.applyPassword(databaseUser, localNewPassword);
            }
        }
    }

    /**
     * Everything of the user as DTO including expiraton date and payload.
     * 
     * @param user
     * @return The given user as DTO with all fields.
     */
    public UserDto userAsDto(User user) {
        PersonalSettings settings = user.getProfileConfiguration();
        UserDto dto = lowerPermService.userAsDto(user);
        dto.settings.payload = settings.getPayload(); // is not done with Default user permissions
        dto.expirationDate = user.getExpirationDate().orElse(null);
        return dto;
    }

    @Override
    public <T extends User> void setPermissionProvider(User user) {
        // TODO Auto-generated method stub

    }
}