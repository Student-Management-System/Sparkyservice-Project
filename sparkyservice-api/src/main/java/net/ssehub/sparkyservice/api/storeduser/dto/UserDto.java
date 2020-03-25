package net.ssehub.sparkyservice.api.storeduser.dto;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.UserRole;
import net.ssehub.sparkyservice.api.storeduser.exceptions.MissingDataException;
import net.ssehub.sparkyservice.db.user.StoredUser;

public class UserDto implements Serializable {

    private static final long serialVersionUID = 4775708076296820879L;

    public static class ChangePasswordDto {
        public String oldPassword;

        @NotBlank
        public String newPassword; 
    }

    /**
     * Takes a {@link UserDto} object and changed the value of the given user. This happens recursive (it will
     * change {@link SettingsDto} and {@link ChangePasswordDto} as well). <br>
     * Does not support changing the realm.
     * 
     * @param databaseUser user which values should be changed
     * @param userDto transfer object which holds the new data
     * @return the StoredUser with changed values
     * @throws MissingDataException is thrown when the given transfer object is not valid (especially 
     *         if anything is null)
     */
    public static @Nonnull StoredUser defaultUserDtoEdit(@Nonnull StoredUser databaseUser, @Nonnull UserDto userDto ) 
                                                   throws MissingDataException {
        return editUserFromDto(databaseUser, userDto, false);
    }

    public static @Nonnull StoredUser adminUserDtoEdit(@Nonnull StoredUser databaseUser, 
                                                       @Nonnull UserDto userDto) throws MissingDataException {
        return editUserFromDto(databaseUser, userDto, true);
    }
    
    private static @Nonnull StoredUser editUserFromDto(@Nonnull StoredUser databaseUser, @Nonnull UserDto userDto, 
                                                       boolean adminMode) throws MissingDataException {
        if (userDto.settings != null && userDto.username != null) {
            databaseUser = SettingsDto.applyPersonalSettings(databaseUser, notNull(userDto.settings));
            databaseUser.setUserName(notNull(userDto.username));
            boolean changePassword = userDto.passwordDto != null 
                    && databaseUser.getRealm() == StoredUserDetails.DEFAULT_REALM;
            boolean adminPassChange = adminMode && userDto.passwordDto.newPassword != null;
            if (changePassword) {
                defaultApplyNewPasswordFromDto(databaseUser, userDto.passwordDto);
            } else if (adminPassChange) {
                adminApplyNewPasswordFromDto(databaseUser, userDto.passwordDto.newPassword);
            }
            if (adminMode && userDto.role != null) {
                databaseUser.setRole(notNull(userDto.role));
            }
            return databaseUser;
        } else {
            throw new MissingDataException("EditUserDto is not valid. Something was null");
        }
    }

    /**
     * Try to apply a new password to the given user. The {@link ChangePasswordDto#oldPassword} must
     * match the one which is already stored in the database. Otherwise the password won't be changed.
     * 
     * @param databaseUserDetails user who's password should be changed
     * @param passwordDto contains old and new password (both values can be null)
     */
    public static void defaultApplyNewPasswordFromDto(@Nullable StoredUser databaseUser,
                                                      @Nullable UserDto.ChangePasswordDto passwordDto) {
        if (passwordDto != null && databaseUser != null) {
            @Nullable String newPassword = passwordDto.newPassword;
            @Nullable String oldPassword = passwordDto.oldPassword;
            if (newPassword != null && oldPassword != null) {
                applyPasswordFromDto(databaseUser, newPassword, oldPassword, false);
            }
        }
    }

    /**
     * Apply a new password to the given user. 
     * 
     * @param databaseUser User where the password should be changed
     * @param newPassword New raw password
     */
    public static void adminApplyNewPasswordFromDto(@Nullable StoredUser databaseUser,
                                                    @Nullable String newPassword) {
        if (databaseUser != null && newPassword != null) {
            applyPasswordFromDto(databaseUser, newPassword, "", true);
        }
    }

    /**
     * Changes the password of a given use with the given password DTO. <br>
     * In case of an admin mode, only a new password must be provided in order to succeed.
     * 
     * @param databaseUser User to edit
     * @param newPassword New raw password (necessary)
     * @param oldPassword The old hashed password (only on non adminEdits necessary)
     * @param adminEdit Decide if the old password must match with the new one
     */
    private static void applyPasswordFromDto(@Nonnull StoredUser databaseUser, @Nonnull String newPassword,
                                             @Nonnull String oldPassword, boolean adminEdit) {
        if (!newPassword.isBlank()) {
            StoredUserDetails localUser;
            if (databaseUser instanceof StoredUserDetails) {
                localUser = (StoredUserDetails) databaseUser;
                if (adminEdit || localUser.getEncoder().matches(oldPassword, localUser.getPassword())) {
                    localUser.encodeAndSetPassword(newPassword);
                } 
            } else {
                localUser = new StoredUserDetails(databaseUser);
                localUser.encodeAndSetPassword(newPassword);
                databaseUser.setPasswordEntity(localUser.getPasswordEntity()); // make pass by reference possible.
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

    public UserRole role;
}
