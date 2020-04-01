package net.ssehub.sparkyservice.api.user.dto;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.validation.ValidPassword;

public class NewUserDto {

    /**
     * Performs a transformation from DTO object to a StoredUserDetails. 
     * 
     * @param newUser valid DTO (username and password required)
     * @return user with the values of the DTO
     */
    public static LocalUserDetails transformToUser(@Nonnull NewUserDto newUser) {
        String username = newUser.username;
        String password = newUser.password;
        if (username != null && password != null) {
            var storedUser =  LocalUserDetails.createStoredLocalUser(username, password, true);
            storedUser.setRole(notNull(newUser.role));
            final var settings = newUser.personalSettings;
            if (settings != null) {                
                SettingsDto.applyPersonalSettings(storedUser, settings);
            }
            return storedUser;
        } else {
            throw new IllegalArgumentException("The NewUserDto hast null values which are not allowed");
        }
    }

    @NotBlank
    public String username;

    @NotNull
    @ValidPassword
    public String password; 

    public UserRole role;

    /**
     * Optional settings. If they aren't provided by the request body a new default set of settings will be generated.
     */
    @Valid
    private SettingsDto personalSettings;

    /**
     * Getter for Spring validation framework
     */
    public SettingsDto getPersonalSettings() {
        return personalSettings;
    }

    /**
     * Setter for Spring validation framework
     */
    public void setPersonalSettings(SettingsDto personalSettings) {
        this.personalSettings = personalSettings;
    }
}
