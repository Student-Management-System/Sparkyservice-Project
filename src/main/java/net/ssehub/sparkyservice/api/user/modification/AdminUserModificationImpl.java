package net.ssehub.sparkyservice.api.user.modification;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Utility class for modifying users with admin permissions.
 * 
 * @author marcel
 */
@Service
class AdminUserModificationImpl implements UserModificationService {

    @Autowired
    private final UserModificationService lowerPermService;

    /**
     * An admin modification service. Set a lower permissions service where this implementation can inherit permissions
     * from.
     * 
     * @param lowerPermService
     */
    @Autowired
    public AdminUserModificationImpl(UserModificationService lowerPermService) {
        this.lowerPermService = lowerPermService;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void update(@Nullable SparkyUser user, @Nullable UserDto userDto) {
        if (user != null && userDto != null) {
            final SettingsDto localDtoSettings = userDto.settings;
            final var pwDto = userDto.passwordDto;
            if (localDtoSettings != null) {
                user.setUsername(userDto.username);
                if (pwDto != null) {
                    user.updatePassword(pwDto, UserRole.ADMIN);                    
                }
                user.setExpireDate(userDto.expirationDate);
                user.setFullname(userDto.fullName);
                user.setRole(userDto.role);
                DefaultUserModificationImpl.applyPersonalSettingsDto(user, localDtoSettings);
                user.getSettings().setPayload(userDto.settings.payload);
            }
        }
    }

    /**
     * Everything of the user as DTO including expiraton date and payload.
     * 
     * @param user
     * @return The given user as DTO with all fields.
     */
    public UserDto asDto(SparkyUser user) {
        PersonalSettings settings = user.getSettings();
        UserDto dto = lowerPermService.asDto(user);
        dto.settings.payload = settings.getPayload(); // is not done with Default user permissions
        dto.expirationDate = user.getExpireDate().orElse(null);
        return dto;
    }
}