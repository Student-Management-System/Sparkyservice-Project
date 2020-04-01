package net.ssehub.sparkyservice.api.user.dto;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;

public class UserDto implements Serializable {

    private static final long serialVersionUID = 4775708076296820879L;

    public static class ChangePasswordDto {
        public String oldPassword;

        @NotBlank
        public String newPassword; 
    }

    @NotBlank
    public String username;

    @NotNull
    public UserRealm realm;

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
