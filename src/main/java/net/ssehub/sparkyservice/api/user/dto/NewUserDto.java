package net.ssehub.sparkyservice.api.user.dto;


import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.validation.ValidPassword;

//CHECKSTYLE:OFF
public class NewUserDto {
    @NotBlank
    public String username;

    @NotNull
    @ValidPassword
    public String password; 

    @NotNull
    public UserRole role;

    /**
     * Optional settings. If they aren't provided by the request body a new default set of settings will be generated.
     */
    @Valid
    public SettingsDto personalSettings;

    public LocalDate expirationTime;

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
