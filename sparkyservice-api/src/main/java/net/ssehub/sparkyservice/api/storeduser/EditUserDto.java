package net.ssehub.sparkyservice.api.storeduser;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.db.user.StoredUser;

public class EditUserDto {
    public static class ChangePasswordDto {
        @NotBlank
        public String oldPassword;

        @NotBlank
        public String newPassword; 
    }

    public static StoredUser editUserFromDtoValues(@Nonnull StoredUser databaseUser, @Nonnull EditUserDto userDto ) 
            throws MissingDataException {
        
        if (userDto.settings != null && userDto.username != null) {
            databaseUser = SettingsDto.writePersonalSettings(databaseUser, notNull(userDto.settings)); 
            databaseUser.setUserName(notNull(userDto.username));
            boolean changePassword = userDto.passwordDto != null 
                    && databaseUser.getRealm() == StoredUserDetails.DEFAULT_REALM;
            if (changePassword) {
                var localUser = new StoredUserDetails(databaseUser);
                changePasswordFromDto(localUser, userDto.passwordDto);
                databaseUser = localUser.getTransactionObject();
            }
            return databaseUser;
        } else {
            throw new MissingDataException("EditUserDto is not valid. Something was null");
        }
    }

    public static void changePasswordFromDto(@Nonnull StoredUserDetails databaseUserDetails,
            @Nullable EditUserDto.ChangePasswordDto passwordDto)
            throws MissingDataException {
        
        String newPassword = Optional.ofNullable(passwordDto).orElseThrow(
                () -> new MissingDataException("Passwords does not match")).newPassword;
        String oldPassword = Optional.ofNullable(passwordDto).orElseThrow(
                () -> new MissingDataException("Passwords does not match")).oldPassword;
        if (newPassword != null && oldPassword != null) {
            boolean oldPasswordIsRight = databaseUserDetails
                    .getEncoder()
                    .matches(oldPassword, databaseUserDetails.getPassword());
            if (oldPasswordIsRight) {
                databaseUserDetails.encodeAndSetPassword(newPassword);
            }
        }
    }

    @NotBlank
    public String username;

    @NotBlank
    public String realm;

    /**
     * Not necessary. Won't have any effect if the user is in any other realm than the local one (like LDAP). 
     */
    @Valid
    public ChangePasswordDto passwordDto;

    @Valid
    @NotNull
    public SettingsDto settings;
}
