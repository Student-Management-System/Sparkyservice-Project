package net.ssehub.sparkyservice.api.storeduser.dto;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.storeduser.MissingDataException;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.db.user.StoredUser;

public class EditUserDto {
    
    public static class ChangePasswordDto {
        @NotBlank
        public String oldPassword;

        @NotBlank
        public String newPassword; 
    }

    /**
     * Takes a {@link EditUserDto} object and changed the value of the given user. This happens recursive (it will
     * change {@link SettingsDto} and {@link ChangePasswordDto} as well). <br>
     * Does not support changing the realm.
     * 
     * @param databaseUser user which values should be changed
     * @param userDto transfer object which holds the new data
     * @return the StoredUser with changed values
     * @throws MissingDataException is thrown when the given transfer object is not valid (especially if anything is null)
     */
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

    /**
     * Changes the password of the given user with the given passwordDto. The {@link ChangePasswordDto#oldPassword} must
     * match the one which is already stored in the database. Otherwise the password won't be changed.
     * 
     * @param databaseUserDetails user who's password should be changed
     * @param passwordDto contains old and new password (both values can be null)
     * @throws MissingDataException is thrown if the DTO is null or invalid
     */
    public static void changePasswordFromDto(@Nonnull StoredUserDetails databaseUserDetails,
            @Nullable EditUserDto.ChangePasswordDto passwordDto) throws MissingDataException {
        
        if (passwordDto != null) {
            @Nullable String newPassword = passwordDto.newPassword;
            @Nullable String oldPassword = passwordDto.oldPassword;
            if (newPassword != null && oldPassword != null) {
                boolean oldPasswordIsRight = databaseUserDetails
                        .getEncoder()
                        .matches(oldPassword, databaseUserDetails.getPassword());
                if (oldPasswordIsRight) {
                    databaseUserDetails.encodeAndSetPassword(newPassword);
                }
            }
        } else {
            throw new MissingDataException("PasswordDto is null");
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
