package net.ssehub.sparkyservice.api.storeduser;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import net.ssehub.sparkyservice.api.validation.ValidPassword;

public class NewUserDto {
    public static StoredUserDetails transformToUser(NewUserDto newUser) {
        return StoredUserDetails.createStoredLocalUser(newUser.username, newUser.password, true);
    }
    
    @NotBlank
    public String username;
    
    @ValidPassword
    @NotBlank
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
